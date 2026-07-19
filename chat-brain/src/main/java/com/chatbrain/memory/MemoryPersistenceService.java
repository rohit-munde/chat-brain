package com.chatbrain.memory;

import com.chatbrain.entity.MemoryCategory;
import com.chatbrain.entity.MemoryConfidence;
import com.chatbrain.entity.MemorySource;
import com.chatbrain.entity.MemoryType;
import com.chatbrain.entity.User;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class MemoryPersistenceService {

	private final MemoryService memoryService;

	public MemoryPersistenceService(MemoryService memoryService) {
		this.memoryService = memoryService;
	}

	public Memory persist(
			User user,
			MemoryCategory category,
			String content,
			Integer importance,
			MemorySource source) {
		com.chatbrain.entity.Memory savedMemory = memoryService.createMemory(
				user,
				toMemoryType(category),
				requireContent(content),
				toConfidence(importance),
				toMemorySource(source));
		return toMemory(savedMemory);
	}

	public List<Memory> retrieveRecent(User user) {
		return memoryService.findRecentMemoriesForUser(user).stream()
				.map(this::toMemory)
				.toList();
	}

	public boolean exists(User user, MemoryCategory category, String content) {
		return memoryService.exists(
				user,
				toMemoryType(category),
				requireContent(content));
	}

	public Memory update(
			User user,
			UUID memoryId,
			MemoryCategory category,
			String content,
			Integer importance,
			MemorySource source) {
		return toMemory(memoryService.updateMemory(
				user,
				memoryId,
				toMemoryType(category),
				requireContent(content),
				toConfidence(importance),
				toMemorySource(source)));
	}

	private String requireContent(String content) {
		if (content == null || content.isBlank()) {
			throw new IllegalArgumentException("content must not be blank");
		}
		return content.trim();
	}

	private Memory toMemory(com.chatbrain.entity.Memory entity) {
		return new Memory(
				entity.getId(),
				entity.getType().name(),
				entity.getValue(),
				entity.getCreatedAt());
	}

	private MemoryType toMemoryType(MemoryCategory category) {
		return switch (Objects.requireNonNull(category, "category must not be null")) {
			case REAL_NAME, IDENTITY -> MemoryType.NAME;
			case CAREER -> MemoryType.PROFESSION;
			case TECHNOLOGY -> MemoryType.LANGUAGE;
			case INTEREST -> MemoryType.INTEREST;
			case PROJECT -> MemoryType.PROJECT;
			case ACHIEVEMENT -> MemoryType.ACHIEVEMENT;
			case PREFERENCE -> MemoryType.PREFERENCE;
			default -> MemoryType.CUSTOM;
		};
	}

	private MemoryConfidence toConfidence(Integer importance) {
		if (importance == null || importance == 2) {
			return MemoryConfidence.MEDIUM;
		}
		return importance < 2 ? MemoryConfidence.LOW : MemoryConfidence.HIGH;
	}

	private MemorySource toMemorySource(MemorySource source) {
		return Objects.requireNonNull(source, "source must not be null");
	}
}
