package com.chatbrain.ai;

import com.chatbrain.events.ChatMessageEvent;
import com.chatbrain.memory.Memory;
import com.chatbrain.memory.MemoryRetriever;
import com.chatbrain.memory.MemoryLearningService;
import com.chatbrain.platform.youtube.YouTubePublisher;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.doThrow;

class AIOrchestratorTests {

	private static final Instant MESSAGE_TIME = Instant.parse("2026-07-18T00:00:00Z");

	@Test
	void retrievesMemoriesBeforeBuildingPromptAndPublishingReply() {
		MemoryRetriever memoryRetriever = mock(MemoryRetriever.class);
		PromptBuilder promptBuilder = mock(PromptBuilder.class);
		LLMClient llmClient = mock(LLMClient.class);
		YouTubePublisher youtubePublisher = mock(YouTubePublisher.class);
		MemoryLearningService memoryLearningService = mock(MemoryLearningService.class);
		ChatMessageEvent event = discordMessage();
		List<Memory> memories = List.of(memory("PREFERENCE", "Likes Java"));
		when(memoryRetriever.retrieve(event)).thenReturn(memories);
		when(promptBuilder.build(event, memories)).thenReturn("prompt");
		when(llmClient.generateReply("prompt")).thenReturn("AI response");
		AIOrchestrator orchestrator = new AIOrchestrator(
				memoryRetriever,
				promptBuilder,
				llmClient,
				youtubePublisher,
				memoryLearningService);

		orchestrator.process(event);

		InOrder workflow = inOrder(
				memoryRetriever, promptBuilder, llmClient, youtubePublisher, memoryLearningService);
		workflow.verify(memoryRetriever).retrieve(event);
		workflow.verify(promptBuilder).build(event, memories);
		workflow.verify(llmClient).generateReply("prompt");
		workflow.verify(youtubePublisher).publish("AI response");
		workflow.verify(memoryLearningService).learn(event, "AI response");
	}

	@Test
	void memoryLearningFailureDoesNotInterruptPublishedReply() {
		MemoryRetriever memoryRetriever = mock(MemoryRetriever.class);
		PromptBuilder promptBuilder = mock(PromptBuilder.class);
		LLMClient llmClient = mock(LLMClient.class);
		YouTubePublisher youtubePublisher = mock(YouTubePublisher.class);
		MemoryLearningService memoryLearningService = mock(MemoryLearningService.class);
		ChatMessageEvent event = discordMessage();
		when(memoryRetriever.retrieve(event)).thenReturn(List.of());
		when(promptBuilder.build(event, List.of())).thenReturn("prompt");
		when(llmClient.generateReply("prompt")).thenReturn("AI response");
		doThrow(new IllegalStateException("database unavailable"))
				.when(memoryLearningService).learn(event, "AI response");
		AIOrchestrator orchestrator = new AIOrchestrator(
				memoryRetriever,
				promptBuilder,
				llmClient,
				youtubePublisher,
				memoryLearningService);

		orchestrator.process(event);

		InOrder workflow = inOrder(youtubePublisher, memoryLearningService);
		workflow.verify(youtubePublisher).publish("AI response");
		workflow.verify(memoryLearningService).learn(event, "AI response");
	}

	@Test
	void buildsPromptWithRelevantMemoriesAndKeepsFakeClientBehaviour() {
		PromptBuilder promptBuilder = new PromptBuilder();
		List<Memory> memories = List.of(
				memory("PREFERENCE", "Likes Java"),
				memory("INTEREST", "Uses Spring Boot"));

		String prompt = promptBuilder.build(discordMessage(), memories);

		assertThat(prompt)
				.contains("Platform: DISCORD")
				.contains("Username: yashmishra")
				.contains("Display Name: Yash Mishra")
				.contains("Timestamp: 2026-07-18T00:00:00Z")
				.contains("Relevant Memories")
				.contains("- [PREFERENCE] Likes Java")
				.contains("- [INTEREST] Uses Spring Boot")
				.contains("Current Message:")
				.contains("How do I deploy Spring Boot?")
				.contains("Respond as the AI co-host of the livestream.");
		assertThat(new FakeLLMClient().generateReply(prompt))
				.isEqualTo("AI Response: How do I deploy Spring Boot?");
	}

	private Memory memory(String category, String content) {
		return new Memory(category, content, MESSAGE_TIME);
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
