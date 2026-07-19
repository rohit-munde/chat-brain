package com.chatbrain.memory;

import com.chatbrain.entity.MemoryConfidence;
import com.chatbrain.entity.MemorySource;
import com.chatbrain.entity.MemoryType;
import com.chatbrain.entity.User;
import com.chatbrain.repository.MemoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class MemoryService {

	private final MemoryRepository memoryRepository;

	public MemoryService(MemoryRepository memoryRepository) {
		this.memoryRepository = memoryRepository;
	}

	@Transactional
	public com.chatbrain.entity.Memory createMemory(
			User user,
			MemoryType type,
			String value,
			MemoryConfidence confidence,
			MemorySource source) {
		requirePersistedUser(user);
		return memoryRepository.save(new com.chatbrain.entity.Memory(
				user, type, value, confidence, source));
	}

	@Transactional(readOnly = true)
	public List<com.chatbrain.entity.Memory> findMemoriesForUser(User user) {
		return List.copyOf(memoryRepository.findByUserIdOrderByCreatedAtDesc(
				requirePersistedUser(user)));
	}

	@Transactional(readOnly = true)
	public List<com.chatbrain.entity.Memory> findRecentMemoriesForUser(User user) {
		return List.copyOf(memoryRepository.findTop5ByUserIdOrderByCreatedAtDesc(
				requirePersistedUser(user)));
	}

	@Transactional(readOnly = true)
	public List<com.chatbrain.entity.Memory> findByType(User user, MemoryType type) {
		return List.copyOf(memoryRepository.findByUserIdAndTypeOrderByCreatedAtDesc(
				requirePersistedUser(user),
				Objects.requireNonNull(type, "type must not be null")));
	}

	@Transactional
	public com.chatbrain.entity.Memory updateMemory(
			User user,
			UUID memoryId,
			MemoryType type,
			String value,
			MemoryConfidence confidence,
			MemorySource source) {
		com.chatbrain.entity.Memory memory = findOwnedMemory(user, memoryId);
		memory.update(type, value, confidence, source);
		return memoryRepository.save(memory);
	}

	@Transactional
	public void deleteMemory(User user, UUID memoryId) {
		memoryRepository.delete(findOwnedMemory(user, memoryId));
	}

	@Transactional(readOnly = true)
	public boolean exists(User user, MemoryType type, String value) {
		return memoryRepository.existsByUserIdAndTypeAndValueIgnoreCase(
				requirePersistedUser(user),
				Objects.requireNonNull(type, "type must not be null"),
				requireValue(value));
	}

	private com.chatbrain.entity.Memory findOwnedMemory(User user, UUID memoryId) {
		return memoryRepository.findByIdAndUserId(
				Objects.requireNonNull(memoryId, "memoryId must not be null"),
				requirePersistedUser(user))
				.orElseThrow(() -> new IllegalArgumentException(
						"Memory was not found for the specified user"));
	}

	private UUID requirePersistedUser(User user) {
		Objects.requireNonNull(user, "user must not be null");
		if (user.getId() == null) {
			throw new IllegalArgumentException("user must be persisted before managing memories");
		}
		return user.getId();
	}

	private String requireValue(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("value must not be blank");
		}
		return value.trim();
	}
}
