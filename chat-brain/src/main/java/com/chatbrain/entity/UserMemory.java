package com.chatbrain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_memories")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserMemory {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Enumerated(EnumType.STRING)
	private MemoryCategory category;

	@Column(columnDefinition = "TEXT")
	private String content;

	private Integer importance;

	@Enumerated(EnumType.STRING)
	private MemorySource source;

	private Instant createdAt;

	@PrePersist
	void initializeCreatedAt() {
		if (createdAt == null) {
			createdAt = Instant.now();
		}
	}
}
