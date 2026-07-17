package com.chatbrain.platform.youtube;

import java.time.Instant;
import java.util.Objects;

public record LiveChatSession(
		String broadcastId,
		String liveChatId,
		Instant discoveredAt) {

	public LiveChatSession {
		if (liveChatId == null || liveChatId.isBlank()) {
			throw new IllegalArgumentException("liveChatId must not be blank");
		}
		Objects.requireNonNull(discoveredAt, "discoveredAt must not be null");
	}
}
