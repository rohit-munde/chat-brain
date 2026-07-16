package com.chatbrain.events;

import java.util.Objects;

public final class ChatMessageEvent extends BaseEvent {

	private final String platform;
	private final String username;
	private final String message;

	public ChatMessageEvent(String platform, String username, String message) {
		super(EventType.CHAT_MESSAGE);
		this.platform = Objects.requireNonNull(platform, "platform must not be null");
		this.username = Objects.requireNonNull(username, "username must not be null");
		this.message = Objects.requireNonNull(message, "message must not be null");
	}

	public String getPlatform() {
		return platform;
	}

	public String getUsername() {
		return username;
	}

	public String getMessage() {
		return message;
	}
}
