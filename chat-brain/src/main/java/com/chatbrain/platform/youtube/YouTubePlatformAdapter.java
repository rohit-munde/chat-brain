package com.chatbrain.platform.youtube;

import com.chatbrain.events.ChatMessageEvent;
import com.chatbrain.platform.Platform;
import com.chatbrain.platform.PlatformAdapter;
import com.chatbrain.platform.PlatformMessage;
import com.chatbrain.platform.events.PlatformMessageMapper;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.LiveBroadcast;
import com.google.api.services.youtube.model.LiveBroadcastListResponse;
import com.google.api.services.youtube.model.LiveChatMessage;
import com.google.api.services.youtube.model.LiveChatMessageAuthorDetails;
import com.google.api.services.youtube.model.LiveChatMessageListResponse;
import com.google.api.services.youtube.model.LiveChatMessageSnippet;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

@Component
@ConditionalOnProperty(name = "chatbrain.youtube.enabled", havingValue = "true")
public final class YouTubePlatformAdapter implements PlatformAdapter {

	private static final Logger LOGGER = LoggerFactory.getLogger(YouTubePlatformAdapter.class);
	private static final long RETRY_DELAY_MILLIS = 5_000L;

	private final YouTube youtube;
	private final PlatformMessageMapper messageMapper;
	private final ApplicationEventPublisher eventPublisher;
	private final AtomicBoolean running = new AtomicBoolean(false);
	private ExecutorService pollingExecutor;

	public YouTubePlatformAdapter(
			YouTube youtube,
			PlatformMessageMapper messageMapper,
			ApplicationEventPublisher eventPublisher) {
		this.youtube = youtube;
		this.messageMapper = messageMapper;
		this.eventPublisher = eventPublisher;
	}

	@Override
	public Platform getPlatform() {
		return Platform.YOUTUBE;
	}

	@Override
	@EventListener(ApplicationReadyEvent.class)
	public synchronized void start() {
		if (!running.compareAndSet(false, true)) {
			return;
		}

		LOGGER.info("Starting YouTube adapter...");
		pollingExecutor = Executors.newSingleThreadExecutor(runnable -> {
			Thread thread = new Thread(runnable, "youtube-live-chat-poller");
			thread.setDaemon(true);
			return thread;
		});
		pollingExecutor.submit(this::pollActiveLiveChat);
	}

	@Override
	@PreDestroy
	public synchronized void stop() {
		boolean wasRunning = running.getAndSet(false);
		if (!wasRunning && pollingExecutor == null) {
			return;
		}

		if (wasRunning) {
			LOGGER.info("Stopping YouTube adapter...");
		}
		if (pollingExecutor != null) {
			pollingExecutor.shutdownNow();
			pollingExecutor = null;
		}
	}

	private void pollActiveLiveChat() {
		while (shouldContinuePolling()) {
			try {
				String liveChatId = findActiveLiveChatId();
				if (liveChatId == null) {
					waitBeforeRetry();
					continue;
				}

				LOGGER.info("Connected to active YouTube livestream chat");
				pollMessages(liveChatId);
				if (shouldContinuePolling()) {
					waitBeforeRetry();
				}
			} catch (GoogleJsonResponseException exception) {
				logApiFailure("discovering the active livestream", exception);
				waitBeforeRetry();
			} catch (IOException exception) {
				logNetworkFailure("discovering the active livestream", exception);
				waitBeforeRetry();
			}
		}
	}

	private String findActiveLiveChatId() throws IOException {
		LiveBroadcastListResponse response = youtube.liveBroadcasts()
				.list(List.of("snippet"))
				.setBroadcastStatus("active")
				.execute();

		List<LiveBroadcast> activeBroadcasts = Optional.ofNullable(response.getItems())
				.orElseGet(List::of);
		if (activeBroadcasts.isEmpty()) {
			LOGGER.warn("No active YouTube livestream was found; discovery will be retried");
			return null;
		}

		String liveChatId = activeBroadcasts.stream()
				.map(LiveBroadcast::getSnippet)
				.filter(snippet -> snippet != null)
				.map(snippet -> snippet.getLiveChatId())
				.filter(candidateChatId -> candidateChatId != null && !candidateChatId.isBlank())
				.findFirst()
				.orElse(null);
		if (liveChatId == null) {
			LOGGER.warn("An active YouTube livestream was found, but live chat is unavailable; discovery will be retried");
		}
		return liveChatId;
	}

	private void pollMessages(String liveChatId) {
		String nextPageToken = null;
		boolean initialPage = true;

		while (shouldContinuePolling()) {
			try {
				LiveChatMessageListResponse response = requestMessages(liveChatId, nextPageToken);
				if (!initialPage) {
					publishMessages(response);
				}
				initialPage = false;
				nextPageToken = response.getNextPageToken();
				waitForNextPoll(response.getPollingIntervalMillis());
			} catch (GoogleJsonResponseException exception) {
				logApiFailure("polling live chat", exception);
				return;
			} catch (IOException exception) {
				logNetworkFailure("polling live chat", exception);
				waitBeforeRetry();
			}
		}
	}

	private LiveChatMessageListResponse requestMessages(String liveChatId, String nextPageToken)
			throws IOException {
		return youtube.liveChatMessages()
				.list(liveChatId, List.of("snippet", "authorDetails"))
				.setPageToken(nextPageToken)
				.execute();
	}

	private void publishMessages(LiveChatMessageListResponse response) {
		Optional.ofNullable(response.getItems())
				.orElseGet(List::of)
				.forEach(this::publishMessage);
	}

	private void publishMessage(LiveChatMessage liveChatMessage) {
		LiveChatMessageSnippet snippet = liveChatMessage.getSnippet();
		LiveChatMessageAuthorDetails author = liveChatMessage.getAuthorDetails();
		if (snippet == null || author == null || snippet.getDisplayMessage() == null) {
			return;
		}

		Instant timestamp = snippet.getPublishedAt() == null
				? Instant.now()
				: Instant.ofEpochMilli(snippet.getPublishedAt().getValue());
		PlatformMessage platformMessage = new PlatformMessage(
				Platform.YOUTUBE,
				author.getChannelId(),
				author.getDisplayName(),
				snippet.getDisplayMessage(),
				timestamp);

		LOGGER.info("""
				----------------------------------
				Platform : {}
				Channel ID : {}
				Visible Name : {}
				Message : {}
				Timestamp : {}
				----------------------------------""",
				platformMessage.platform(),
				platformMessage.channelId(),
				platformMessage.visibleName(),
				platformMessage.message(),
				platformMessage.timestamp());

		try {
			ChatMessageEvent event = messageMapper.toChatMessageEvent(platformMessage);
			eventPublisher.publishEvent(event);
		} catch (RuntimeException exception) {
			LOGGER.error("Failed to publish YouTube chat message event for channel {}",
					platformMessage.channelId(), exception);
		}
	}

	private boolean shouldContinuePolling() {
		return running.get() && !Thread.currentThread().isInterrupted();
	}

	private void waitForNextPoll(Long pollingIntervalMillis) throws IOException {
		if (pollingIntervalMillis == null) {
			throw new IOException("YouTube response did not include pollingIntervalMillis");
		}
		waitFor(pollingIntervalMillis);
	}

	private void waitBeforeRetry() {
		waitFor(RETRY_DELAY_MILLIS);
	}

	private void waitFor(long delayMillis) {
		try {
			Thread.sleep(delayMillis);
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
		}
	}

	private void logApiFailure(String operation, GoogleJsonResponseException exception) {
		String details = exception.getDetails() == null
				? exception.getMessage()
				: exception.getDetails().getMessage();
		LOGGER.error("YouTube API failure while {} (HTTP {}): {}. The operation will be retried when possible",
				operation, exception.getStatusCode(), details);
	}

	private void logNetworkFailure(String operation, IOException exception) {
		LOGGER.warn("Temporary network failure while {}; polling will continue: {}",
				operation, exception.getMessage());
	}
}
