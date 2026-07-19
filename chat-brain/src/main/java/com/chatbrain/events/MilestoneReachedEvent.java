package com.chatbrain.events;

import java.time.Instant;

public final class MilestoneReachedEvent extends AbstractProactiveStreamEvent {
	public MilestoneReachedEvent(String summary) { super(EventType.MILESTONE_REACHED, summary); }
	public MilestoneReachedEvent(String summary, Instant timestamp) { super(EventType.MILESTONE_REACHED, summary, timestamp); }
}
