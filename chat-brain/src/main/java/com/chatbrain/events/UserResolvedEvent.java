package com.chatbrain.events;

import com.chatbrain.entity.PlatformIdentity;
import com.chatbrain.entity.User;

import java.util.Objects;

public final class UserResolvedEvent extends BaseEvent {

	private final User user;
	private final PlatformIdentity platformIdentity;
	private final ChatMessageEvent originalEvent;

	public UserResolvedEvent(
			User user,
			PlatformIdentity platformIdentity,
			ChatMessageEvent originalEvent) {
		super(EventType.USER_RESOLVED);
		this.user = Objects.requireNonNull(user, "user must not be null");
		this.platformIdentity = Objects.requireNonNull(
				platformIdentity,
				"platformIdentity must not be null");
		this.originalEvent = Objects.requireNonNull(originalEvent, "originalEvent must not be null");
	}

	public User getUser() {
		return user;
	}

	public PlatformIdentity getPlatformIdentity() {
		return platformIdentity;
	}

	public ChatMessageEvent getOriginalEvent() {
		return originalEvent;
	}
}
