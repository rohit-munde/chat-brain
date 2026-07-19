package com.chatbrain.platform.youtube;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.LiveChatMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class YouTubePublisherTests {

	private YouTube.LiveChatMessages.Insert insertRequest;
	private YouTube.LiveChatMessages liveChatMessages;
	private YouTubePublisher publisher;

	@BeforeEach
	@SuppressWarnings("unchecked")
	void setUp() throws IOException {
		YouTube youtube = mock(YouTube.class);
		liveChatMessages = mock(YouTube.LiveChatMessages.class);
		insertRequest = mock(YouTube.LiveChatMessages.Insert.class);
		LiveChatSessionManager sessionManager = mock(LiveChatSessionManager.class);
		ObjectProvider<YouTube> youtubeProvider = mock(ObjectProvider.class);
		ObjectProvider<LiveChatSessionManager> sessionManagerProvider = mock(ObjectProvider.class);

		when(youtubeProvider.getIfAvailable()).thenReturn(youtube);
		when(sessionManagerProvider.getIfAvailable()).thenReturn(sessionManager);
		when(sessionManager.currentSession()).thenReturn(Optional.of(
				new LiveChatSession("broadcast-123", "chat-123", Instant.now())));
		when(youtube.liveChatMessages()).thenReturn(liveChatMessages);
		when(liveChatMessages.insert(eq(List.of("snippet")), any(LiveChatMessage.class)))
				.thenReturn(insertRequest);
		when(insertRequest.execute()).thenReturn(new LiveChatMessage().setId("published-123"));

		publisher = new YouTubePublisher(youtubeProvider, sessionManagerProvider);
	}

	@Test
	void publishesToCurrentYouTubeSessionAndMarksOwnMessage() throws IOException {
		publisher.publish("AI Response: Hello");

		ArgumentCaptor<LiveChatMessage> messageCaptor = ArgumentCaptor.forClass(LiveChatMessage.class);
		verify(liveChatMessages).insert(eq(List.of("snippet")), messageCaptor.capture());
		verify(insertRequest).execute();
		assertThat(messageCaptor.getValue().getSnippet().getLiveChatId()).isEqualTo("chat-123");
		assertThat(messageCaptor.getValue().getSnippet().getTextMessageDetails().getMessageText())
				.isEqualTo("AI Response: Hello");
		assertThat(publisher.isCommunityBrainMessage("published-123")).isTrue();
		assertThat(publisher.isCommunityBrainMessage("published-123")).isFalse();
		assertThat(publisher.isCommunityBrainMessage(null, "AI Response: Hello")).isTrue();
		assertThat(publisher.isCommunityBrainMessage(null, "AI Response: Hello")).isFalse();
	}
}
