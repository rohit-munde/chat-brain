package com.chatbrain.proactive;

import com.chatbrain.ai.PromptBuilder;
import com.chatbrain.events.BuildSucceededEvent;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProactivePromptBuilderTests {

	@Test
	void includesOnlyProvidedStreamContextAndCommentContract() {
		BuildSucceededEvent event = new BuildSucceededEvent(
				"Maven build succeeded", Instant.parse("2026-07-19T12:00:00Z"));
		ProactiveCommentaryContext context = new ProactiveCommentaryContext(
				event,
				"Building ChatBrain live",
				"ChatBrain",
				"Proactive commentary",
				List.of(new StreamTimelineEntry(
						Instant.parse("2026-07-19T11:55:00Z"),
						event.getEventType(),
						"Compilation fixed")),
				List.of("Rohit: the tests are green"),
				List.of("The compiler has ended negotiations."));

		String prompt = new PromptBuilder().buildProactive(context);

		assertThat(prompt)
				.contains("Conversation Personality V1")
				.contains("Stream Title: Building ChatBrain live")
				.contains("Current Project: ChatBrain")
				.contains("Current Coding Topic: Proactive commentary")
				.contains("Recent Timeline")
				.contains("Compilation fixed")
				.contains("Recent Chat Summary")
				.contains("Rohit: the tests are green")
				.contains("Recent AI Comments")
				.contains("The compiler has ended negotiations.")
				.contains("Type: BUILD_SUCCEEDED")
				.contains("Summary: Maven build succeeded")
				.contains("{\"action\":\"COMMENT\"")
				.contains("{\"action\":\"IGNORE\"}");
	}
}
