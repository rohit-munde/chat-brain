package com.chatbrain.events;

import com.chatbrain.platform.Platform;

import java.util.Objects;

public final class MemberJoinedEvent extends BaseEvent {

	private final Platform platform;
	private final String platformUserId;
	private final String username;
	private final String displayName;

	public MemberJoinedEvent(
			Platform platform,
			String platformUserId,
			String username,
			String displayName) {
		super(EventType.MEMBER_JOINED);
		this.platform = Objects.requireNonNull(platform, "platform must not be null");
		this.platformUserId = Objects.requireNonNull(
				platformUserId,
				"platformUserId must not be null");
		this.username = Objects.requireNonNull(username, "username must not be null");
		this.displayName = Objects.requireNonNull(displayName, "displayName must not be null");
	}

	public Platform getPlatform() {
		return platform;
	}

	public String getPlatformUserId() {
		return platformUserId;
	}

	public String getUsername() {
		return username;
	}

	public String getDisplayName() {
		return displayName;
	}
}
