package com.chatbrain.platform.youtube.metrics;

import com.chatbrain.ai.AIResponseAction;
import com.chatbrain.ai.AIResponseDecision;
import com.chatbrain.ai.AIResponseDecisionObserver;
import com.chatbrain.events.ChatMessageEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class YouTubeApiMetricsService implements AIResponseDecisionObserver {

	private static final String METRIC_PREFIX = "chatbrain.youtube";

	private final Map<YouTubeApiEndpoint, EndpointMeters> endpointMeters =
			new EnumMap<>(YouTubeApiEndpoint.class);
	private final Counter messagesReceived;
	private final Counter messagesIgnored;
	private final Counter repliesPublished;
	private final Counter broadcastDiscoveries;
	private final Counter uniqueAuthorEnrichments;
	private final Counter authorCacheHits;
	private final Counter authorCacheMisses;
	private final AtomicLong currentPollingIntervalMillis = new AtomicLong();

	public YouTubeApiMetricsService(MeterRegistry meterRegistry) {
		for (YouTubeApiEndpoint endpoint : YouTubeApiEndpoint.values()) {
			endpointMeters.put(endpoint, createEndpointMeters(meterRegistry, endpoint));
		}

		messagesReceived = counter(meterRegistry, "messages.received",
				"YouTube chat messages accepted into the application pipeline");
		messagesIgnored = counter(meterRegistry, "messages.ignored",
				"YouTube chat messages ignored by the AI response decision");
		repliesPublished = counter(meterRegistry, "replies.published",
				"AI replies successfully published to YouTube");
		broadcastDiscoveries = counter(meterRegistry, "broadcasts.discovered",
				"Active YouTube broadcasts successfully discovered");
		uniqueAuthorEnrichments = counter(meterRegistry, "authors.enriched",
				"Unique YouTube authors enriched during this process");
		authorCacheHits = counter(meterRegistry, "author.cache.hits",
				"YouTube author identity cache hits");
		authorCacheMisses = counter(meterRegistry, "author.cache.misses",
				"YouTube author identity cache misses");

		Gauge.builder(METRIC_PREFIX + ".polling.interval.milliseconds",
				currentPollingIntervalMillis, AtomicLong::get)
				.description("Current effective YouTube live-chat polling interval")
				.register(meterRegistry);
	}

	public <T> T recordApiCall(YouTubeApiEndpoint endpoint, ApiCall<T> apiCall)
			throws IOException {
		EndpointMeters meters = endpointMeters.get(endpoint);
		meters.requests().increment();
		long startedAt = System.nanoTime();
		try {
			T response = apiCall.execute();
			meters.successes().increment();
			return response;
		} catch (IOException | RuntimeException exception) {
			meters.failures().increment();
			throw exception;
		} finally {
			meters.latency().record(System.nanoTime() - startedAt, TimeUnit.NANOSECONDS);
		}
	}

	public void recordPollingInterval(long intervalMillis) {
		currentPollingIntervalMillis.set(intervalMillis);
	}

	public void recordMessageReceived() {
		messagesReceived.increment();
	}

	public void recordReplyPublished() {
		repliesPublished.increment();
	}

	public void recordBroadcastDiscovery() {
		broadcastDiscoveries.increment();
	}

	public void recordAuthorCacheHit() {
		authorCacheHits.increment();
	}

	public void recordAuthorCacheMiss() {
		authorCacheMisses.increment();
	}

	public void recordUniqueAuthorEnrichment() {
		uniqueAuthorEnrichments.increment();
	}

	@Override
	public void onDecisionExecuted(ChatMessageEvent event, AIResponseDecision decision) {
		if (isYouTubeEvent(event) && decision.action() == AIResponseAction.IGNORE) {
			messagesIgnored.increment();
		}
	}

	public YouTubeApiMetrics snapshot() {
		Map<YouTubeApiEndpoint, YouTubeApiMetrics.EndpointMetrics> endpoints =
				new EnumMap<>(YouTubeApiEndpoint.class);
		endpointMeters.forEach((endpoint, meters) -> endpoints.put(
				endpoint,
				new YouTubeApiMetrics.EndpointMetrics(
						counterValue(meters.requests()),
						counterValue(meters.successes()),
						counterValue(meters.failures()),
						meters.latency().mean(TimeUnit.MILLISECONDS),
						meters.latency().totalTime(TimeUnit.MILLISECONDS))));

		return new YouTubeApiMetrics(
				endpoints,
				currentPollingIntervalMillis.get(),
				counterValue(messagesReceived),
				counterValue(messagesIgnored),
				counterValue(repliesPublished),
				counterValue(broadcastDiscoveries),
				counterValue(uniqueAuthorEnrichments),
				counterValue(authorCacheHits),
				counterValue(authorCacheMisses));
	}

	public String formatSnapshot() {
		YouTubeApiMetrics metrics = snapshot();
		return """
				YouTube Metrics

				Requests
				--------
				%s

				Polling
				-------
				Current interval: %d ms

				Messages
				--------
				Received: %d
				Ignored: %d
				Replies published: %d

				Identity
				--------
				Unique enrichments: %d
				Cache hits: %d
				Cache misses: %d

				Broadcasts discovered: %d

				Latency
				-------
				Average poll request: %.2f ms
				Average insert request: %.2f ms
				""".formatted(
				formatEndpointRequests(metrics),
				metrics.currentPollingIntervalMillis(),
				metrics.messagesReceived(),
				metrics.messagesIgnored(),
				metrics.repliesPublished(),
				metrics.uniqueAuthorEnrichments(),
				metrics.authorCacheHits(),
				metrics.authorCacheMisses(),
				metrics.broadcastDiscoveries(),
				averageLatency(metrics, YouTubeApiEndpoint.LIVE_CHAT_MESSAGES_LIST),
				averageLatency(metrics, YouTubeApiEndpoint.LIVE_CHAT_MESSAGES_INSERT));
	}

	private EndpointMeters createEndpointMeters(
			MeterRegistry meterRegistry,
			YouTubeApiEndpoint endpoint) {
		String endpointTag = endpoint.metricTag();
		return new EndpointMeters(
				Counter.builder(METRIC_PREFIX + ".api.requests")
						.description("YouTube Data API requests")
						.tag("endpoint", endpointTag)
						.register(meterRegistry),
				Counter.builder(METRIC_PREFIX + ".api.successes")
						.description("Successful YouTube Data API requests")
						.tag("endpoint", endpointTag)
						.register(meterRegistry),
				Counter.builder(METRIC_PREFIX + ".api.failures")
						.description("Failed YouTube Data API requests")
						.tag("endpoint", endpointTag)
						.register(meterRegistry),
				Timer.builder(METRIC_PREFIX + ".api.latency")
						.description("YouTube Data API request latency")
						.tag("endpoint", endpointTag)
						.register(meterRegistry));
	}

	private Counter counter(MeterRegistry meterRegistry, String suffix, String description) {
		return Counter.builder(METRIC_PREFIX + "." + suffix)
				.description(description)
				.register(meterRegistry);
	}

	private boolean isYouTubeEvent(ChatMessageEvent event) {
		return event.getPlatform() != null
				&& "YOUTUBE".equals(event.getPlatform().trim().toUpperCase(Locale.ROOT));
	}

	private long counterValue(Counter counter) {
		return Math.round(counter.count());
	}

	private String formatEndpointRequests(YouTubeApiMetrics metrics) {
		StringBuilder output = new StringBuilder();
		for (YouTubeApiEndpoint endpoint : YouTubeApiEndpoint.values()) {
			YouTubeApiMetrics.EndpointMetrics endpointMetrics = metrics.endpoints().get(endpoint);
			output.append(endpoint.metricTag())
					.append(": ")
					.append(endpointMetrics.requests())
					.append(" (success=")
					.append(endpointMetrics.successes())
					.append(", failure=")
					.append(endpointMetrics.failures())
					.append(")\n");
		}
		return output.toString().stripTrailing();
	}

	private double averageLatency(YouTubeApiMetrics metrics, YouTubeApiEndpoint endpoint) {
		return metrics.endpoints().get(endpoint).averageLatencyMillis();
	}

	@FunctionalInterface
	public interface ApiCall<T> {

		T execute() throws IOException;
	}

	private record EndpointMeters(
			Counter requests,
			Counter successes,
			Counter failures,
			Timer latency) {
	}
}
