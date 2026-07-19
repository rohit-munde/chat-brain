package com.chatbrain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(
		name = "user_memories",
		indexes = {
			@Index(name = "idx_user_memories_user_created_at", columnList = "user_id, created_at"),
			@Index(name = "idx_user_memories_user_type", columnList = "user_id, category")
		})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Memory {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Convert(converter = MemoryTypeConverter.class)
	@Column(name = "category", nullable = false)
	private MemoryType type;

	@Column(name = "content", nullable = false, columnDefinition = "TEXT")
	private String value;

	@Convert(converter = MemoryConfidenceConverter.class)
	@Column(name = "importance")
	private MemoryConfidence confidence;

	@Convert(converter = MemorySourceConverter.class)
	@Column(nullable = false)
	private MemorySource source;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	public Memory(
			User user,
			MemoryType type,
			String value,
			MemoryConfidence confidence,
			MemorySource source) {
		this.user = Objects.requireNonNull(user, "user must not be null");
		this.type = Objects.requireNonNull(type, "type must not be null");
		this.value = requireValue(value);
		this.confidence = Objects.requireNonNull(confidence, "confidence must not be null");
		this.source = Objects.requireNonNull(source, "source must not be null");
	}

	public void update(
			MemoryType type,
			String value,
			MemoryConfidence confidence,
			MemorySource source) {
		this.type = Objects.requireNonNull(type, "type must not be null");
		this.value = requireValue(value);
		this.confidence = Objects.requireNonNull(confidence, "confidence must not be null");
		this.source = Objects.requireNonNull(source, "source must not be null");
	}

	@PrePersist
	void initializeTimestamps() {
		Instant now = Instant.now();
		if (createdAt == null) {
			createdAt = now;
		}
		updatedAt = now;
	}

	@PreUpdate
	void updateTimestamp() {
		updatedAt = Instant.now();
	}

	private static String requireValue(String value) {
		if (value == null || value.isBlank()) {
			throw new IllegalArgumentException("value must not be blank");
		}
		return value.trim();
	}
}
