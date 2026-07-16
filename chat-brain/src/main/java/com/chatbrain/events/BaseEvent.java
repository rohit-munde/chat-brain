package com.chatbrain.events;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public abstract class BaseEvent {

	private final UUID eventId;
	private final Instant timestamp;
	private final EventType eventType;

	protected BaseEvent(EventType eventType) {
		this.eventId = UUID.randomUUID();
		this.timestamp = Instant.now();
		this.eventType = Objects.requireNonNull(eventType, "eventType must not be null");
	}

	public UUID getEventId() {
		return eventId;
	}

	public Instant getTimestamp() {
		return timestamp;
	}

	public EventType getEventType() {
		return eventType;
	}
}
