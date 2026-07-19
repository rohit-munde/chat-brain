package com.chatbrain.comedy;

import com.chatbrain.proactive.StreamTimelineEntry;

import java.util.List;

public record ComedyContext(
		boolean comedyOpportunity,
		ComedyOpportunity opportunity,
		ComedyStyle recommendedStyle,
		String streamMood,
		String currentProject,
		String currentTopic,
		String currentCodingPhase,
		String culturalGuidance,
		List<ComedyThemeCount> activeThemes,
		List<String> activeCallbacks,
		List<StreamTimelineEntry> recentTimeline,
		List<String> recentChat,
		List<String> recentAiComments,
		List<String> qualityChecklist,
		String fewShotExamples) {

	public ComedyContext {
		activeThemes = List.copyOf(activeThemes);
		activeCallbacks = List.copyOf(activeCallbacks);
		recentTimeline = List.copyOf(recentTimeline);
		recentChat = List.copyOf(recentChat);
		recentAiComments = List.copyOf(recentAiComments);
		qualityChecklist = List.copyOf(qualityChecklist);
	}

	public static ComedyContext none() {
		return new ComedyContext(
				false, ComedyOpportunity.NONE, ComedyStyle.NO_HUMOR,
				"Not provided", "Not provided", "Not provided", "Not provided",
				"No cultural reference is needed.", List.of(), List.of(), List.of(),
				List.of(), List.of(), List.of(), "");
	}
}
