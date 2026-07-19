package com.chatbrain.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class MemoryTypeConverter implements AttributeConverter<MemoryType, String> {

	@Override
	public String convertToDatabaseColumn(MemoryType type) {
		return type == null ? null : type.name();
	}

	@Override
	public MemoryType convertToEntityAttribute(String value) {
		if (value == null) {
			return null;
		}
		return switch (value) {
			case "NAME", "REAL_NAME", "IDENTITY" -> MemoryType.NAME;
			case "PROFESSION", "CAREER" -> MemoryType.PROFESSION;
			case "LANGUAGE", "TECHNOLOGY" -> MemoryType.LANGUAGE;
			case "INTEREST" -> MemoryType.INTEREST;
			case "PROJECT" -> MemoryType.PROJECT;
			case "ACHIEVEMENT" -> MemoryType.ACHIEVEMENT;
			case "PREFERENCE" -> MemoryType.PREFERENCE;
			default -> MemoryType.CUSTOM;
		};
	}
}
