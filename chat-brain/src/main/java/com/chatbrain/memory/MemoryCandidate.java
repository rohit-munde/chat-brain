package com.chatbrain.memory;

import com.chatbrain.entity.MemoryCategory;

import java.util.Objects;

public record MemoryCandidate(MemoryCategory category, String content) {

	public MemoryCandidate {
		Objects.requireNonNull(category, "category must not be null");
		if (content == null || content.isBlank()) {
			throw new IllegalArgumentException("content must not be blank");
		}
		content = content.trim();
	}
}
