package com.chatbrain.platform;

import java.time.Instant;

public record PlatformMessage(
		Platform platform,
		String channelId,
		String visibleName,
		String message,
		Instant timestamp) {
}
