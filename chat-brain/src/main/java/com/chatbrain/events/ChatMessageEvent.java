package com.chatbrain.events;

import java.time.Instant;
import java.util.Objects;

public final class ChatMessageEvent extends BaseEvent {

	private final String platform;
	private final String platformUserId;
	private final String handle;
	private final String displayName;
	private final String message;

	public ChatMessageEvent(String platform, String platformUserId, String message) {
		this(platform, platformUserId, null, platformUserId, message);
	}

	public ChatMessageEvent(
			String platform,
			String platformUserId,
			String handle,
			String displayName,
			String message) {
		this(platform, platformUserId, handle, displayName, message, Instant.now());
	}

	public ChatMessageEvent(
			String platform,
			String platformUserId,
			String handle,
			String displayName,
			String message,
			Instant timestamp) {
		super(EventType.CHAT_MESSAGE, timestamp);
		this.platform = Objects.requireNonNull(platform, "platform must not be null");
		this.platformUserId = platformUserId;
		this.handle = handle;
		this.displayName = displayName;
		this.message = Objects.requireNonNull(message, "message must not be null");
	}

	public String getPlatform() {
		return platform;
	}

	public String getPlatformUserId() {
		return platformUserId;
	}

	public String getHandle() {
		return handle;
	}

	public String getDisplayName() {
		return displayName;
	}

	public String getMessage() {
		return message;
	}
}
