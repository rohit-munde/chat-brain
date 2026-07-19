package com.chatbrain.repository;

import com.chatbrain.entity.Memory;
import com.chatbrain.entity.MemoryType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MemoryRepository extends JpaRepository<Memory, UUID> {

	List<Memory> findByUserIdOrderByCreatedAtDesc(UUID userId);

	List<Memory> findTop5ByUserIdOrderByCreatedAtDesc(UUID userId);

	List<Memory> findByUserIdAndTypeOrderByCreatedAtDesc(UUID userId, MemoryType type);

	Optional<Memory> findByIdAndUserId(UUID id, UUID userId);

	boolean existsByUserIdAndTypeAndValueIgnoreCase(
			UUID userId,
			MemoryType type,
			String value);
}
