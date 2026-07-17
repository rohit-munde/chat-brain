package com.chatbrain.ai;

import com.chatbrain.events.ChatMessageEvent;
import com.chatbrain.platform.youtube.YouTubePublisher;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class AIOrchestratorTests {

	private static final Instant MESSAGE_TIME = Instant.parse("2026-07-18T00:00:00Z");

	@Test
	void buildsPlatformNeutralPromptAndPublishesFakeReplyToYouTube() {
		PromptBuilder promptBuilder = new PromptBuilder();
		YouTubePublisher youtubePublisher = mock(YouTubePublisher.class);
		AIOrchestrator orchestrator = new AIOrchestrator(
				promptBuilder,
				new FakeLLMClient(),
				youtubePublisher);
		ChatMessageEvent event = discordMessage();

		String prompt = promptBuilder.build(event);
		orchestrator.process(event);

		assertThat(prompt)
				.contains("Platform: DISCORD")
				.contains("Username: yashmishra")
				.contains("Display Name: Yash Mishra")
				.contains("Timestamp: 2026-07-18T00:00:00Z")
				.contains("How do I deploy Spring Boot?")
				.contains("Respond as the AI co-host of the livestream.");
		verify(youtubePublisher).publish("AI Response: How do I deploy Spring Boot?");
	}

	private ChatMessageEvent discordMessage() {
		return new ChatMessageEvent(
				"DISCORD",
				"discord-user-123",
				"yashmishra",
				"Yash Mishra",
				"How do I deploy Spring Boot?",
				MESSAGE_TIME);
	}
}
