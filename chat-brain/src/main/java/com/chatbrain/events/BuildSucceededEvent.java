package com.chatbrain.events;

import java.time.Instant;

public final class BuildSucceededEvent extends AbstractProactiveStreamEvent {
	public BuildSucceededEvent(String summary) { super(EventType.BUILD_SUCCEEDED, summary); }
	public BuildSucceededEvent(String summary, Instant timestamp) { super(EventType.BUILD_SUCCEEDED, summary, timestamp); }
}
