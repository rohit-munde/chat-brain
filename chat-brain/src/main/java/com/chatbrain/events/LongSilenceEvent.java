package com.chatbrain.events;

import java.time.Instant;

public final class LongSilenceEvent extends AbstractProactiveStreamEvent {
	public LongSilenceEvent(String summary) { super(EventType.LONG_SILENCE, summary); }
	public LongSilenceEvent(String summary, Instant timestamp) { super(EventType.LONG_SILENCE, summary, timestamp); }
}
