package com.chatbrain.platform;

import java.time.Instant;

public record PlatformMessage(
		Platform platform,
		String platformUserId,
		String handle,
		String displayName,
		String message,
		Instant timestamp) {
}
