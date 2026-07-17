package com.chatbrain.memory;

import java.time.Instant;
import java.util.Objects;

public record Memory(String category, String content, Instant createdAt) {

	public Memory {
		Objects.requireNonNull(category, "category must not be null");
		Objects.requireNonNull(content, "content must not be null");
		Objects.requireNonNull(createdAt, "createdAt must not be null");
	}
}
