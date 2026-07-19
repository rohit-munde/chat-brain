package com.chatbrain.proactive;

import com.chatbrain.events.ProactiveStreamEvent;
import org.springframework.stereotype.Service;

import java.util.ArrayDeque;
import java.util.List;

@Service
public class StreamTimeline {

	private static final int MAX_ENTRIES = 50;
	private final ArrayDeque<StreamTimelineEntry> entries = new ArrayDeque<>();

	public synchronized void append(ProactiveStreamEvent event) {
		entries.addLast(new StreamTimelineEntry(
				event.getTimestamp(), event.getEventType(), event.getSummary()));
		while (entries.size() > MAX_ENTRIES) {
			entries.removeFirst();
		}
	}

	public synchronized List<StreamTimelineEntry> recentEntries(int limit) {
		if (limit <= 0) {
			return List.of();
		}
		List<StreamTimelineEntry> snapshot = List.copyOf(entries);
		return snapshot.subList(Math.max(0, snapshot.size() - limit), snapshot.size());
	}
}
