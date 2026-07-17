package com.chatbrain.memory;

import com.chatbrain.entity.MemoryCategory;
import com.chatbrain.events.ChatMessageEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class RuleBasedMemoryExtractorTests {

	private final RuleBasedMemoryExtractor extractor = new RuleBasedMemoryExtractor();

	@Test
	void extractsSupportedFactsWithoutComplexInference() {
		ChatMessageEvent event = event(
				"My name is Rohit. I use Spring Boot and I like Java. "
						+ "I'm building CommunityBrain and I live in Pune.");

		assertThat(extractor.extract(event, "reply"))
				.containsExactlyInAnyOrder(
						new MemoryCandidate(MemoryCategory.IDENTITY, "My name is Rohit"),
						new MemoryCandidate(MemoryCategory.TECHNOLOGY, "I use Spring Boot"),
						new MemoryCandidate(MemoryCategory.PREFERENCE, "I like Java"),
						new MemoryCandidate(MemoryCategory.PROJECT, "I'm building CommunityBrain"),
						new MemoryCandidate(MemoryCategory.LOCATION, "I live in Pune"));
	}

	@Test
	void returnsNoMemoriesForOrdinaryConversation() {
		assertThat(extractor.extract(event("How do I deploy Spring Boot?"), "reply")).isEmpty();
	}

	private ChatMessageEvent event(String message) {
		return new ChatMessageEvent(
				"DISCORD",
				"user-123",
				"rohit",
				"Rohit",
				message,
				Instant.parse("2026-07-18T00:00:00Z"));
	}
}
