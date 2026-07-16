package com.chatbrain.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	private String displayName;

	private String realName;

	private Integer relationshipScore = 0;

	private Instant firstSeen;

	private Instant lastSeen;

	private Instant createdAt;

	private Instant updatedAt;

	@PrePersist
	void initializeTimestamps() {
		Instant now = Instant.now();
		if (firstSeen == null) {
			firstSeen = now;
		}
		if (lastSeen == null) {
			lastSeen = now;
		}
		if (createdAt == null) {
			createdAt = now;
		}
		updatedAt = now;
		if (relationshipScore == null) {
			relationshipScore = 0;
		}
	}

	@PreUpdate
	void updateTimestamp() {
		updatedAt = Instant.now();
	}
}
