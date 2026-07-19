package com.chatbrain.events;

import java.time.Instant;

public final class FeatureCompletedEvent extends AbstractProactiveStreamEvent {
	public FeatureCompletedEvent(String summary) { super(EventType.FEATURE_COMPLETED, summary); }
	public FeatureCompletedEvent(String summary, Instant timestamp) { super(EventType.FEATURE_COMPLETED, summary, timestamp); }
}
