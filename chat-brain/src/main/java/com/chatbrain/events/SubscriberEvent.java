package com.chatbrain.events;

import com.chatbrain.platform.Platform;

import java.time.Instant;
import java.util.Objects;

public final class SubscriberEvent extends BaseEvent implements StreamEvent {

	private final Platform platform;
	private final String platformUserId;
	private final String displayName;
	private final String handle;

	public SubscriberEvent(
			Platform platform,
			String platformUserId,
			String displayName,
			String handle,
			Instant timestamp) {
		super(EventType.SUBSCRIBER, timestamp);
		this.platform = Objects.requireNonNull(platform, "platform must not be null");
		this.platformUserId = requireText(platformUserId, "platformUserId");
		this.displayName = requireText(displayName, "displayName");
		this.handle = handle;
	}

	public Platform getPlatform() {
		return platform;
	}

	public String getPlatformUserId() {
		return platformUserId;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getHandle() {
		return handle;
	}

	private static String requireText(String value, String fieldName) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException(fieldName + " must not be blank");
		}
		return value;
	}
}
