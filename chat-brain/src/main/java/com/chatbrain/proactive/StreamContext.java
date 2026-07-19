package com.chatbrain.proactive;

import com.chatbrain.events.ChatMessageEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.List;

@Service
public class StreamContext {

	private static final int MAX_RECENT_ITEMS = 20;
	private final ProactiveCommentaryProperties properties;
	private final ArrayDeque<String> recentChatActivity = new ArrayDeque<>();
	private final ArrayDeque<String> recentAiComments = new ArrayDeque<>();

	public StreamContext(ProactiveCommentaryProperties properties) {
		this.properties = properties;
	}

	@EventListener
	public synchronized void onChatMessage(ChatMessageEvent event) {
		append(recentChatActivity, "%s: %s".formatted(event.getDisplayName(), event.getMessage()));
	}

	public synchronized void recordAiComment(String comment) {
		append(recentAiComments, comment);
	}

	public synchronized ProactiveCommentaryContext snapshot(
			com.chatbrain.events.ProactiveStreamEvent event,
			List<StreamTimelineEntry> timeline) {
		return new ProactiveCommentaryContext(
				event,
				properties.getStreamTitle(),
				properties.getCurrentProject(),
				properties.getCurrentCodingTopic(),
				timeline,
				List.copyOf(recentChatActivity),
				List.copyOf(recentAiComments));
	}

	private void append(ArrayDeque<String> items, String value) {
		items.addLast(value);
		while (items.size() > MAX_RECENT_ITEMS) {
			items.removeFirst();
		}
	}
}
