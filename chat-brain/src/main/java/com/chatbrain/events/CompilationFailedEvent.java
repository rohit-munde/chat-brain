package com.chatbrain.events;

import java.time.Instant;

public final class CompilationFailedEvent extends AbstractProactiveStreamEvent {
	public CompilationFailedEvent(String summary) { super(EventType.COMPILATION_FAILED, summary); }
	public CompilationFailedEvent(String summary, Instant timestamp) { super(EventType.COMPILATION_FAILED, summary, timestamp); }
}
