package com.chatbrain.proactive;

import com.chatbrain.events.ProactiveStreamEvent;

import java.util.List;

public record ProactiveCommentaryContext(
		ProactiveStreamEvent currentEvent,
		String streamTitle,
		String currentProject,
		String currentCodingTopic,
		List<StreamTimelineEntry> recentTimeline,
		List<String> recentChatSummary,
		List<String> recentAiComments) {

	public ProactiveCommentaryContext {
		recentTimeline = List.copyOf(recentTimeline);
		recentChatSummary = List.copyOf(recentChatSummary);
		recentAiComments = List.copyOf(recentAiComments);
	}
}
