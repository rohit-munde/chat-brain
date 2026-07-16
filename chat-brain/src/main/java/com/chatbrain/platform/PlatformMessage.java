package com.chatbrain.platform;

import java.time.Instant;

public record PlatformMessage(
		Platform platform,
		String username,
		String displayName,
		String message,
		Instant timestamp) {
}
