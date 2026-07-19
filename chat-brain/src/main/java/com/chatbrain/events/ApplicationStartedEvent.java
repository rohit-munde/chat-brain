package com.chatbrain.events;

import java.time.Instant;

public final class ApplicationStartedEvent extends AbstractProactiveStreamEvent {
	public ApplicationStartedEvent(String summary) { super(EventType.APPLICATION_STARTED, summary); }
	public ApplicationStartedEvent(String summary, Instant timestamp) { super(EventType.APPLICATION_STARTED, summary, timestamp); }
}
