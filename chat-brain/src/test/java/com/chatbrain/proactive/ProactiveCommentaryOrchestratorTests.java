package com.chatbrain.proactive;

import com.chatbrain.ai.AIResponseDecisionParser;
import com.chatbrain.ai.LLMClient;
import com.chatbrain.ai.PromptBuilder;
import com.chatbrain.comedy.ComedyContext;
import com.chatbrain.comedy.ComedyIntelligence;
import com.chatbrain.events.BuildSucceededEvent;
import com.chatbrain.platform.youtube.YouTubePublisher;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProactiveCommentaryOrchestratorTests {

	@Test
	void publishesCommentDecisionAndRecordsTimelineAndMetrics() {
		Fixture fixture = new Fixture(true);
		when(fixture.llmClient.generateReply(anyString()))
				.thenReturn("{\"action\":\"COMMENT\",\"reply\":\"Everyone act surprised.\"}");

		fixture.orchestrator.process(new BuildSucceededEvent("Maven build succeeded"));

		verify(fixture.youtubePublisher).publish("Everyone act surprised.");
		assertThat(fixture.timeline.recentEntries(10)).hasSize(1);
		assertThat(fixture.metrics.snapshot().generated()).isEqualTo(1);
		assertThat(fixture.metrics.snapshot().published()).isEqualTo(1);
	}

	@Test
	void ignoreDecisionDoesNotPublish() {
		Fixture fixture = new Fixture(true);
		when(fixture.llmClient.generateReply(anyString()))
				.thenReturn("{\"action\":\"IGNORE\"}");

		fixture.orchestrator.process(new BuildSucceededEvent("Maven build succeeded"));

		verify(fixture.youtubePublisher, never()).publish(anyString());
		assertThat(fixture.metrics.snapshot().skipped()).isEqualTo(1);
	}

	@Test
	void disabledFeatureRecordsTimelineWithoutCallingAi() {
		Fixture fixture = new Fixture(false);

		fixture.orchestrator.process(new BuildSucceededEvent("Maven build succeeded"));

		verify(fixture.llmClient, never()).generateReply(anyString());
		verify(fixture.youtubePublisher, never()).publish(anyString());
		assertThat(fixture.timeline.recentEntries(10)).hasSize(1);
		assertThat(fixture.metrics.snapshot().skipped()).isEqualTo(1);
	}

	@Test
	void consecutiveProactiveEventIsSuppressed() {
		Fixture fixture = new Fixture(true);
		when(fixture.llmClient.generateReply(anyString()))
				.thenReturn("{\"action\":\"COMMENT\",\"reply\":\"First comment\"}");

		fixture.orchestrator.process(new BuildSucceededEvent("First build"));
		fixture.orchestrator.process(new BuildSucceededEvent("Second build"));

		verify(fixture.llmClient).generateReply(anyString());
		assertThat(fixture.metrics.snapshot().skipped()).isEqualTo(1);
	}

	private static final class Fixture {
		private final StreamTimeline timeline = new StreamTimeline();
		private final LLMClient llmClient = mock(LLMClient.class);
		private final YouTubePublisher youtubePublisher = mock(YouTubePublisher.class);
		private final ProactiveCommentaryMetrics metrics =
				new ProactiveCommentaryMetrics(new SimpleMeterRegistry());
		private final ProactiveCommentaryOrchestrator orchestrator;

		private Fixture(boolean enabled) {
			ProactiveCommentaryProperties properties = new ProactiveCommentaryProperties();
			properties.setEnabled(enabled);
			properties.setMinimumCooldown(Duration.ZERO);
			StreamContext streamContext = new StreamContext(properties);
			ProactiveCommentaryGuard guard = new ProactiveCommentaryGuard(properties);
			ComedyIntelligence comedyIntelligence = mock(ComedyIntelligence.class);
			when(comedyIntelligence.analyze(org.mockito.ArgumentMatchers.any(BuildSucceededEvent.class)))
					.thenReturn(ComedyContext.none());
			orchestrator = new ProactiveCommentaryOrchestrator(
					properties,
					timeline,
					streamContext,
					guard,
					comedyIntelligence,
					new PromptBuilder(),
					llmClient,
					new AIResponseDecisionParser(new ObjectMapper()),
					youtubePublisher,
					metrics);
		}
	}
}
