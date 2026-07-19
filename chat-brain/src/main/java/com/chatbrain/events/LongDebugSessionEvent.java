package com.chatbrain.events;

import java.time.Instant;

public final class LongDebugSessionEvent extends AbstractProactiveStreamEvent {
	public LongDebugSessionEvent(String summary) { super(EventType.LONG_DEBUG_SESSION, summary); }
	public LongDebugSessionEvent(String summary, Instant timestamp) { super(EventType.LONG_DEBUG_SESSION, summary, timestamp); }
}
