package com.chatbrain.comedy;

import com.chatbrain.events.ChatMessageEvent;
import com.chatbrain.events.ProactiveStreamEvent;
import com.chatbrain.proactive.ProactiveCommentaryProperties;
import com.chatbrain.proactive.StreamContext;
import com.chatbrain.proactive.StreamTimeline;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ComedyIntelligence {

	private static final int CONTEXT_LIMIT = 8;
	private final ComedyOpportunityDetector opportunityDetector;
	private final ComedyStyleSelector styleSelector;
	private final ComedyMemory comedyMemory;
	private final CulturalReferenceGuide culturalReferenceGuide;
	private final ComedyQualityPolicy qualityPolicy;
	private final ComedyFewShotLibrary fewShotLibrary;
	private final StreamTimeline streamTimeline;
	private final StreamContext streamContext;
	private final ProactiveCommentaryProperties properties;

	public ComedyIntelligence(
			ComedyOpportunityDetector opportunityDetector,
			ComedyStyleSelector styleSelector,
			ComedyMemory comedyMemory,
			CulturalReferenceGuide culturalReferenceGuide,
			ComedyQualityPolicy qualityPolicy,
			ComedyFewShotLibrary fewShotLibrary,
			StreamTimeline streamTimeline,
			StreamContext streamContext,
			ProactiveCommentaryProperties properties) {
		this.opportunityDetector = opportunityDetector;
		this.styleSelector = styleSelector;
		this.comedyMemory = comedyMemory;
		this.culturalReferenceGuide = culturalReferenceGuide;
		this.qualityPolicy = qualityPolicy;
		this.fewShotLibrary = fewShotLibrary;
		this.streamTimeline = streamTimeline;
		this.streamContext = streamContext;
		this.properties = properties;
	}

	public ComedyContext analyze(ChatMessageEvent event) {
		return analyze(event.getMessage(), event.getEventType());
	}

	public ComedyContext analyze(ProactiveStreamEvent event) {
		return analyze(event.getSummary(), event.getEventType());
	}

	public void recordAiMessage(String message) {
		if (message != null && !message.isBlank()) {
			streamContext.recordAiComment(message);
		}
	}

	private ComedyContext analyze(String sourceText, com.chatbrain.events.EventType eventType) {
		List<ComedyThemeCount> themes = comedyMemory.observe(sourceText);
		List<String> callbacks = themes.stream()
				.filter(theme -> theme.count() > 1)
				.filter(theme -> relevantTo(theme.theme(), sourceText))
				.map(theme -> "%s has appeared %d times this stream"
						.formatted(theme.theme().displayName(), theme.count()))
				.toList();
		ComedyOpportunity opportunity = opportunityDetector.detect(sourceText, eventType);
		ComedySituation situation = new ComedySituation(
				opportunity, sourceText, properties.getStreamMood(), !callbacks.isEmpty());
		ComedyStyle style = styleSelector.select(situation);

		return new ComedyContext(
				style != ComedyStyle.NO_HUMOR,
				opportunity,
				style,
				properties.getStreamMood(),
				properties.getCurrentProject(),
				properties.getCurrentCodingTopic(),
				properties.getCurrentCodingPhase(),
				culturalReferenceGuide.guidanceFor(style),
				themes,
				callbacks,
				streamTimeline.recentEntries(CONTEXT_LIMIT),
				streamContext.recentChat(CONTEXT_LIMIT),
				streamContext.recentAiComments(CONTEXT_LIMIT),
				qualityPolicy.checklist(),
				fewShotLibrary.examples());
	}

	private boolean relevantTo(ComedyTheme theme, String sourceText) {
		String value = sourceText == null ? "" : sourceText.toLowerCase(java.util.Locale.ROOT);
		return switch (theme) {
			case DOCKER_SAGA -> value.contains("docker") || value.contains("container");
			case REDIS_SAGA -> value.contains("redis");
			case SPRING_RESTART_COUNTER -> value.contains("spring") && value.contains("restart");
			case COFFEE_COUNTER -> value.contains("coffee") || value.contains("chai");
			case BROWSER_TAB_COUNTER -> value.contains("tab");
			case TODO_COUNTER -> value.contains("todo");
			case BUILD_FAILURE_COUNTER -> value.contains("build") || value.contains("compilation") || value.contains("test");
			case MAVEN_DOWNLOAD_SAGA -> value.contains("maven") || value.contains("dependenc");
			case MERGE_CONFLICT_SAGA -> value.contains("merge") || value.contains("rebase");
		};
	}
}
