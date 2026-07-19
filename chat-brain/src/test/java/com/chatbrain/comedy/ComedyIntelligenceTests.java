package com.chatbrain.comedy;

import com.chatbrain.ai.PromptBuilder;
import com.chatbrain.events.ChatMessageEvent;
import com.chatbrain.proactive.ProactiveCommentaryProperties;
import com.chatbrain.proactive.StreamContext;
import com.chatbrain.proactive.StreamTimeline;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ComedyIntelligenceTests {

	private ComedyIntelligence intelligence;

	@BeforeEach
	void setUp() {
		ProactiveCommentaryProperties properties = new ProactiveCommentaryProperties();
		properties.setCurrentProject("CommunityBrain");
		properties.setCurrentCodingTopic("Comedy Intelligence");
		properties.setCurrentCodingPhase("Implementation");
		properties.setStreamMood("Focused");
		StreamTimeline timeline = new StreamTimeline();
		StreamContext streamContext = new StreamContext(properties);
		intelligence = new ComedyIntelligence(
				new ComedyOpportunityDetector(),
				new ComedyStyleSelector(List.of(new ContextualComedyStyleStrategy())),
				new ComedyMemory(),
				new CulturalReferenceGuide(),
				new ComedyQualityPolicy(),
				new ComedyFewShotLibrary(),
				timeline,
				streamContext,
				properties);
	}

	@Test
	void greetingIsNotAComedyOpportunity() {
		ComedyContext context = intelligence.analyze(message("hi"));

		assertThat(context.comedyOpportunity()).isFalse();
		assertThat(context.opportunity()).isEqualTo(ComedyOpportunity.NONE);
		assertThat(context.recommendedStyle()).isEqualTo(ComedyStyle.NO_HUMOR);
	}

	@Test
	void recurringDockerFailureSelectsCallbackInsteadOfAnotherGenericJoke() {
		ComedyContext first = intelligence.analyze(message("Docker failed during deployment"));
		ComedyContext second = intelligence.analyze(message("Docker failed again"));

		assertThat(first.recommendedStyle()).isEqualTo(ComedyStyle.DEVELOPER_HUMOR);
		assertThat(first.activeCallbacks()).isEmpty();
		assertThat(second.recommendedStyle()).isEqualTo(ComedyStyle.CALLBACK_HUMOR);
		assertThat(second.activeCallbacks()).containsExactly("Docker Saga has appeared 2 times this stream");
	}

	@Test
	void indianContextIsSelectedOnlyWhenMessageMakesItRelevant() {
		ComedyContext ordinary = intelligence.analyze(message("The build passed"));
		ComedyContext cultural = intelligence.analyze(message("This feels like an engineering college viva"));

		assertThat(ordinary.recommendedStyle()).isNotEqualTo(ComedyStyle.INDIAN_ENGINEERING_CULTURE);
		assertThat(cultural.recommendedStyle()).isEqualTo(ComedyStyle.INDIAN_ENGINEERING_CULTURE);
		assertThat(cultural.culturalGuidance()).contains("only if the context supports it");
	}

	@Test
	void technicalQuestionUsesTeachingThroughHumorWithoutLosingQualityGate() {
		ComedyContext context = intelligence.analyze(message("Why do we need Flyway?"));

		assertThat(context.recommendedStyle()).isEqualTo(ComedyStyle.TEACHING_THROUGH_HUMOR);
		assertThat(context.qualityChecklist()).contains("Technically accurate?");
		assertThat(context.fewShotExamples())
				.contains("IGNORE")
				.contains("Docker humor")
				.contains("Friendly roast")
				.contains("Callback")
				.contains("Indian engineering culture")
				.contains("Bollywood analogy")
				.contains("Cricket analogy")
				.contains("Serious response")
				.contains("Milestone celebration");
	}

	@Test
	void seriousProductionIncidentSuppressesHumor() {
		ComedyContext context = intelligence.analyze(
				message("How do we recover from this production data loss?"));

		assertThat(context.opportunity()).isEqualTo(ComedyOpportunity.SERIOUS_TECHNICAL);
		assertThat(context.comedyOpportunity()).isFalse();
		assertThat(context.recommendedStyle()).isEqualTo(ComedyStyle.NO_HUMOR);
	}

	@Test
	void promptReceivesBoundedComedyGuidanceAndFewShotExamples() {
		ChatMessageEvent event = message("Docker failed again");
		ComedyContext context = intelligence.analyze(event);

		String prompt = new PromptBuilder().build(event, List.of(), context);

		assertThat(prompt)
				.contains("Comedy Intelligence")
				.contains("culturally aware senior Indian software")
				.contains("Opportunity: DOCKER_FAILURE")
				.contains("Recommended Style: DEVELOPER_HUMOR")
				.contains("Current Project: CommunityBrain")
				.contains("Current Coding Phase: Implementation")
				.contains("Comedy Quality Gate")
				.contains("Few-Shot Decision Examples")
				.contains("1. IGNORE")
				.contains("13. Milestone celebration");
	}

	private ChatMessageEvent message(String text) {
		return new ChatMessageEvent(
				"YOUTUBE", "channel-1", "@viewer", "Viewer", text,
				Instant.parse("2026-07-19T12:00:00Z"));
	}
}
