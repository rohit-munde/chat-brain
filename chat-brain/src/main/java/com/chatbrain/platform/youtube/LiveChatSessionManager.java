package com.chatbrain.platform.youtube;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.LiveBroadcast;
import com.google.api.services.youtube.model.LiveBroadcastListResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "chatbrain.youtube.enabled", havingValue = "true")
public class LiveChatSessionManager {

	private static final Logger LOGGER = LoggerFactory.getLogger(LiveChatSessionManager.class);

	private final YouTube youtube;
	private LiveChatSession activeSession;

	public LiveChatSessionManager(YouTube youtube) {
		this.youtube = youtube;
	}

	public synchronized Optional<LiveChatSession> currentSession() throws IOException {
		if (activeSession != null) {
			return Optional.of(activeSession);
		}

		return discoverSession();
	}

	public synchronized Optional<LiveChatSession> discoverSession() throws IOException {
		if (activeSession != null) {
			return Optional.of(activeSession);
		}

		LiveBroadcastListResponse response = youtube.liveBroadcasts()
				.list(List.of("snippet"))
				.setBroadcastStatus("active")
				.execute();

		List<LiveBroadcast> activeBroadcasts = Optional.ofNullable(response.getItems())
				.orElseGet(List::of);
		if (activeBroadcasts.isEmpty()) {
			LOGGER.warn("No active YouTube livestream was found; discovery will be retried");
			return Optional.empty();
		}

		Optional<LiveChatSession> session = activeBroadcasts.stream()
				.filter(broadcast -> broadcast.getSnippet() != null)
				.filter(broadcast -> broadcast.getSnippet().getLiveChatId() != null)
				.filter(broadcast -> !broadcast.getSnippet().getLiveChatId().isBlank())
				.map(broadcast -> new LiveChatSession(
						broadcast.getId(),
						broadcast.getSnippet().getLiveChatId(),
						Instant.now()))
				.findFirst();
		if (session.isEmpty()) {
			LOGGER.warn("An active YouTube livestream was found, but live chat is unavailable; discovery will be retried");
		}
		session.ifPresent(discoveredSession -> activeSession = discoveredSession);
		return session;
	}

	public synchronized void invalidate() {
		if (activeSession != null) {
			LOGGER.info("Invalidating YouTube live-chat session for broadcast {}", activeSession.broadcastId());
		}
		activeSession = null;
	}
}
