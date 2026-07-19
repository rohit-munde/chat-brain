package com.chatbrain.proactive;

import com.chatbrain.events.EventType;

import java.time.Instant;

public record StreamTimelineEntry(Instant timestamp, EventType eventType, String summary) {
}
