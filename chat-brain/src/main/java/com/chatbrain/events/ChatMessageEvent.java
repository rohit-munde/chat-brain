package com.chatbrain.events;

import java.util.Objects;

public final class ChatMessageEvent extends BaseEvent {

	private final String platform;
	private final String channelId;
	private final String visibleName;
	private final String message;

	public ChatMessageEvent(String platform, String channelId, String message) {
		this(platform, channelId, channelId, message);
	}

	public ChatMessageEvent(
			String platform,
			String channelId,
			String visibleName,
			String message) {
		super(EventType.CHAT_MESSAGE);
		this.platform = Objects.requireNonNull(platform, "platform must not be null");
		this.channelId = channelId;
		this.visibleName = visibleName;
		this.message = Objects.requireNonNull(message, "message must not be null");
	}

	public String getPlatform() {
		return platform;
	}

	public String getChannelId() {
		return channelId;
	}

	public String getVisibleName() {
		return visibleName;
	}

	public String getMessage() {
		return message;
	}
}
