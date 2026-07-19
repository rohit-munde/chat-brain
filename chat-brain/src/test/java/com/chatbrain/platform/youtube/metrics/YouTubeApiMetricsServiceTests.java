package com.chatbrain.platform.youtube.metrics;

import com.chatbrain.ai.AIResponseAction;
import com.chatbrain.ai.AIResponseDecision;
import com.chatbrain.events.ChatMessageEvent;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class YouTubeApiMetricsServiceTests {

	private final YouTubeApiMetricsService metricsService =
			new YouTubeApiMetricsService(new SimpleMeterRegistry());

	@Test
	void recordsApiSuccessFailureAndLatency() throws IOException {
		metricsService.recordApiCall(
				YouTubeApiEndpoint.LIVE_CHAT_MESSAGES_LIST,
				() -> "response");

		assertThatThrownBy(() -> metricsService.recordApiCall(
				YouTubeApiEndpoint.LIVE_CHAT_MESSAGES_LIST,
				() -> {
					throw new IOException("network failure");
				}))
				.isInstanceOf(IOException.class);

		YouTubeApiMetrics.EndpointMetrics endpoint = metricsService.snapshot()
				.endpoints()
				.get(YouTubeApiEndpoint.LIVE_CHAT_MESSAGES_LIST);
		assertThat(endpoint.requests()).isEqualTo(2);
		assertThat(endpoint.successes()).isEqualTo(1);
		assertThat(endpoint.failures()).isEqualTo(1);
		assertThat(endpoint.totalLatencyMillis()).isGreaterThanOrEqualTo(0);
		assertThat(endpoint.averageLatencyMillis()).isGreaterThanOrEqualTo(0);
	}

	@Test
	void recordsApplicationMetricsAndFormatsSnapshot() {
		metricsService.recordPollingInterval(10_000);
		metricsService.recordMessageReceived();
		metricsService.recordReplyPublished();
		metricsService.recordBroadcastDiscovery();
		metricsService.recordAuthorCacheHit();
		metricsService.recordAuthorCacheMiss();
		metricsService.recordUniqueAuthorEnrichment();
		metricsService.onDecisionExecuted(
				youtubeMessage(),
				new AIResponseDecision(AIResponseAction.IGNORE, null));

		YouTubeApiMetrics snapshot = metricsService.snapshot();
		assertThat(snapshot.currentPollingIntervalMillis()).isEqualTo(10_000);
		assertThat(snapshot.messagesReceived()).isEqualTo(1);
		assertThat(snapshot.messagesIgnored()).isEqualTo(1);
		assertThat(snapshot.repliesPublished()).isEqualTo(1);
		assertThat(snapshot.broadcastDiscoveries()).isEqualTo(1);
		assertThat(snapshot.uniqueAuthorEnrichments()).isEqualTo(1);
		assertThat(snapshot.authorCacheHits()).isEqualTo(1);
		assertThat(snapshot.authorCacheMisses()).isEqualTo(1);
		assertThat(metricsService.formatSnapshot())
				.contains("YouTube Metrics")
				.contains("liveChatMessages.list: 0")
				.contains("Current interval: 10000 ms")
				.contains("Received: 1")
				.contains("Ignored: 1");
	}

	@Test
	void doesNotCountIgnoredDecisionForAnotherPlatform() {
		ChatMessageEvent discordEvent = new ChatMessageEvent(
				"DISCORD", "user-1", "username", "Display", "message", Instant.now());

		metricsService.onDecisionExecuted(
				discordEvent,
				new AIResponseDecision(AIResponseAction.IGNORE, null));

		assertThat(metricsService.snapshot().messagesIgnored()).isZero();
	}

	private ChatMessageEvent youtubeMessage() {
		return new ChatMessageEvent(
				"YOUTUBE", "channel-1", "@viewer", "Viewer", "message", Instant.now());
	}
}
