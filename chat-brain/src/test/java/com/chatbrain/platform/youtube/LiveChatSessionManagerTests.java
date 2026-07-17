package com.chatbrain.platform.youtube;

import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.LiveBroadcast;
import com.google.api.services.youtube.model.LiveBroadcastListResponse;
import com.google.api.services.youtube.model.LiveBroadcastSnippet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LiveChatSessionManagerTests {

	private YouTube.LiveBroadcasts.List listRequest;
	private LiveChatSessionManager sessionManager;

	@BeforeEach
	void setUp() throws IOException {
		YouTube youtube = mock(YouTube.class);
		YouTube.LiveBroadcasts liveBroadcasts = mock(YouTube.LiveBroadcasts.class);
		listRequest = mock(YouTube.LiveBroadcasts.List.class);
		when(youtube.liveBroadcasts()).thenReturn(liveBroadcasts);
		when(liveBroadcasts.list(List.of("snippet"))).thenReturn(listRequest);
		when(listRequest.setBroadcastStatus("active")).thenReturn(listRequest);
		when(listRequest.execute()).thenReturn(activeBroadcastResponse());
		sessionManager = new LiveChatSessionManager(youtube);
	}

	@Test
	void cachesDiscoveredSessionUntilInvalidated() throws IOException {
		LiveChatSession firstSession = sessionManager.currentSession().orElseThrow();
		LiveChatSession cachedSession = sessionManager.discoverSession().orElseThrow();

		assertThat(cachedSession).isSameAs(firstSession);
		assertThat(firstSession.broadcastId()).isEqualTo("broadcast-123");
		assertThat(firstSession.liveChatId()).isEqualTo("chat-123");
		verify(listRequest).execute();

		sessionManager.invalidate();
		LiveChatSession rediscoveredSession = sessionManager.currentSession().orElseThrow();

		assertThat(rediscoveredSession).isNotSameAs(firstSession);
		verify(listRequest, times(2)).execute();
	}

	private LiveBroadcastListResponse activeBroadcastResponse() {
		LiveBroadcastSnippet snippet = new LiveBroadcastSnippet().setLiveChatId("chat-123");
		LiveBroadcast broadcast = new LiveBroadcast()
				.setId("broadcast-123")
				.setSnippet(snippet);
		return new LiveBroadcastListResponse().setItems(List.of(broadcast));
	}
}
