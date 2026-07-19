package com.chatbrain.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class MemoryConfidenceConverter implements AttributeConverter<MemoryConfidence, Integer> {

	@Override
	public Integer convertToDatabaseColumn(MemoryConfidence confidence) {
		if (confidence == null) {
			return null;
		}
		return switch (confidence) {
			case LOW -> 1;
			case MEDIUM -> 2;
			case HIGH -> 3;
		};
	}

	@Override
	public MemoryConfidence convertToEntityAttribute(Integer value) {
		if (value == null || value == 2) {
			return MemoryConfidence.MEDIUM;
		}
		return value < 2 ? MemoryConfidence.LOW : MemoryConfidence.HIGH;
	}
}
