package com.chatbrain.repository;

import com.chatbrain.entity.MemoryCategory;
import com.chatbrain.entity.UserMemory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserMemoryRepository extends JpaRepository<UserMemory, UUID> {

	List<UserMemory> findTop5ByUserIdOrderByCreatedAtDesc(UUID userId);

	Optional<UserMemory> findByIdAndUserId(UUID id, UUID userId);

	boolean existsByUserIdAndCategoryAndContentIgnoreCase(
			UUID userId,
			MemoryCategory category,
			String content);
}
