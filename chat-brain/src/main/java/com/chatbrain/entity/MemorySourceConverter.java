package com.chatbrain.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class MemorySourceConverter implements AttributeConverter<MemorySource, String> {

	@Override
	public String convertToDatabaseColumn(MemorySource source) {
		return source == null ? null : source.name();
	}

	@Override
	public MemorySource convertToEntityAttribute(String value) {
		if (value == null) {
			return null;
		}
		return switch (value) {
			case "USER", "USER_MESSAGE" -> MemorySource.USER_MESSAGE;
			case "STREAMER", "STREAMER_COMMAND" -> MemorySource.STREAMER_COMMAND;
			case "AI", "AI_INFERENCE" -> MemorySource.AI_INFERENCE;
			case "MANUAL" -> MemorySource.MANUAL;
			default -> throw new IllegalArgumentException("Unsupported memory source: " + value);
		};
	}
}
