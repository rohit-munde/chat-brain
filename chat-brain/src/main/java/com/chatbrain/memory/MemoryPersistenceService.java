package com.chatbrain.memory;

import com.chatbrain.entity.MemoryCategory;
import com.chatbrain.entity.MemorySource;
import com.chatbrain.entity.User;
import com.chatbrain.entity.UserMemory;
import com.chatbrain.repository.UserMemoryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
public class MemoryPersistenceService {

	private final UserMemoryRepository memoryRepository;

	public MemoryPersistenceService(UserMemoryRepository memoryRepository) {
		this.memoryRepository = memoryRepository;
	}

	@Transactional
	public Memory persist(
			User user,
			MemoryCategory category,
			String content,
			Integer importance,
			MemorySource source) {
		UUID userId = requirePersistedUser(user);
		UserMemory entity = new UserMemory(
				user,
				Objects.requireNonNull(category, "category must not be null"),
				requireContent(content),
				importance,
				Objects.requireNonNull(source, "source must not be null"));
		UserMemory savedMemory = memoryRepository.save(entity);
		if (!userId.equals(savedMemory.getUser().getId())) {
			throw new IllegalStateException("Persisted memory owner does not match the requested user");
		}
		return toMemory(savedMemory);
	}

	@Transactional(readOnly = true)
	public List<Memory> retrieveRecent(User user) {
		UUID userId = requirePersistedUser(user);
		return memoryRepository.findTop5ByUserIdOrderByCreatedAtDesc(userId).stream()
				.map(this::toMemory)
				.toList();
	}

	@Transactional
	public Memory update(
			User user,
			UUID memoryId,
			MemoryCategory category,
			String content,
			Integer importance,
			MemorySource source) {
		UUID userId = requirePersistedUser(user);
		UserMemory memory = memoryRepository.findByIdAndUserId(
				Objects.requireNonNull(memoryId, "memoryId must not be null"),
				userId)
				.orElseThrow(() -> new IllegalArgumentException(
						"Memory was not found for the specified user"));
		memory.setCategory(Objects.requireNonNull(category, "category must not be null"));
		memory.setContent(requireContent(content));
		memory.setImportance(importance);
		memory.setSource(Objects.requireNonNull(source, "source must not be null"));
		return toMemory(memoryRepository.save(memory));
	}

	private UUID requirePersistedUser(User user) {
		Objects.requireNonNull(user, "user must not be null");
		if (user.getId() == null) {
			throw new IllegalArgumentException("user must be persisted before storing memories");
		}
		return user.getId();
	}

	private String requireContent(String content) {
		if (content == null || content.isBlank()) {
			throw new IllegalArgumentException("content must not be blank");
		}
		return content;
	}

	private Memory toMemory(UserMemory entity) {
		return new Memory(
				entity.getId(),
				entity.getCategory().name(),
				entity.getContent(),
				entity.getCreatedAt());
	}
}
