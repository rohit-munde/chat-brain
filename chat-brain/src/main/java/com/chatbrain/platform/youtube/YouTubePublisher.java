package com.chatbrain.platform.youtube;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.LiveChatMessage;
import com.google.api.services.youtube.model.LiveChatMessageSnippet;
import com.google.api.services.youtube.model.LiveChatTextMessageDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class YouTubePublisher {

	private static final Logger LOGGER = LoggerFactory.getLogger(YouTubePublisher.class);

	private final ObjectProvider<YouTube> youtubeProvider;
	private final ObjectProvider<LiveChatSessionManager> sessionManagerProvider;
	private final Set<String> publishedMessageIds = ConcurrentHashMap.newKeySet();

	public YouTubePublisher(
			ObjectProvider<YouTube> youtubeProvider,
			ObjectProvider<LiveChatSessionManager> sessionManagerProvider) {
		this.youtubeProvider = youtubeProvider;
		this.sessionManagerProvider = sessionManagerProvider;
	}

	public void publish(String response) {
		if (response == null || response.isBlank()) {
			throw new IllegalArgumentException("YouTube response must not be blank");
		}

		YouTube youtube = youtubeProvider.getIfAvailable();
		LiveChatSessionManager sessionManager = sessionManagerProvider.getIfAvailable();
		if (youtube == null || sessionManager == null) {
			LOGGER.warn("YouTube publishing is unavailable because the YouTube integration is disabled");
			return;
		}

		LOGGER.info("Publishing to YouTube");
		try {
			Optional<LiveChatSession> session = sessionManager.currentSession();
			if (session.isEmpty()) {
				LOGGER.warn("Cannot publish to YouTube because no active live chat is available");
				return;
			}

			LiveChatMessage publishedMessage = youtube.liveChatMessages()
					.insert(List.of("snippet"), createMessage(session.get().liveChatId(), response))
					.execute();
			rememberPublishedMessage(publishedMessage);
			LOGGER.info("Published Successfully");
		} catch (GoogleJsonResponseException exception) {
			String details = exception.getDetails() == null
					? exception.getMessage()
					: exception.getDetails().getMessage();
			LOGGER.error("YouTube API failure while publishing (HTTP {}): {}",
					exception.getStatusCode(), details, exception);
		} catch (IOException exception) {
			LOGGER.error("Failed to publish to YouTube: {}", exception.getMessage(), exception);
		}
	}

	public boolean isCommunityBrainMessage(String messageId) {
		return messageId != null && publishedMessageIds.remove(messageId);
	}

	private LiveChatMessage createMessage(String liveChatId, String response) {
		LiveChatTextMessageDetails textDetails = new LiveChatTextMessageDetails()
				.setMessageText(response);
		LiveChatMessageSnippet snippet = new LiveChatMessageSnippet()
				.setLiveChatId(liveChatId)
				.setType("textMessageEvent")
				.setTextMessageDetails(textDetails);
		return new LiveChatMessage().setSnippet(snippet);
	}

	private void rememberPublishedMessage(LiveChatMessage publishedMessage) {
		if (publishedMessage != null
				&& publishedMessage.getId() != null
				&& !publishedMessage.getId().isBlank()) {
			publishedMessageIds.add(publishedMessage.getId());
		}
	}
}
