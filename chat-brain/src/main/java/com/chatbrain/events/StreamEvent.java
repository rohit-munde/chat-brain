package com.chatbrain.events;

import java.time.Instant;
import java.util.UUID;

public interface StreamEvent {

	UUID getEventId();

	Instant getTimestamp();

	EventType getEventType();
}
