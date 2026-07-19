package com.chatbrain.events;

import java.time.Instant;
import java.util.Objects;

abstract class AbstractProactiveStreamEvent extends BaseEvent implements ProactiveStreamEvent {

	private final String summary;

	protected AbstractProactiveStreamEvent(EventType eventType, String summary) {
		this(eventType, summary, Instant.now());
	}

	protected AbstractProactiveStreamEvent(EventType eventType, String summary, Instant timestamp) {
		super(eventType, timestamp);
		this.summary = requireSummary(summary);
	}

	@Override
	public String getSummary() {
		return summary;
	}

	private String requireSummary(String value) {
		String normalized = Objects.requireNonNull(value, "summary must not be null").trim();
		if (normalized.isEmpty()) {
			throw new IllegalArgumentException("summary must not be blank");
		}
		return normalized;
	}
}
