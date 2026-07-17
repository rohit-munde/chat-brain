package com.chatbrain.entity;

import com.chatbrain.platform.Platform;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
		name = "platform_identities",
		uniqueConstraints = @UniqueConstraint(
				name = "uk_platform_identity_platform_user",
				columnNames = {"platform", "platform_user_id"}))
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlatformIdentity {

	@Id
	@GeneratedValue(strategy = GenerationType.UUID)
	private UUID id;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Platform platform;

	@Column(name = "platform_user_id", nullable = false)
	private String platformUserId;

	@Column(name = "visible_name")
	private String displayName;

	private String handle;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "user_id", nullable = false)
	private User user;

	@Column(nullable = false, updatable = false)
	private Instant createdAt;

	@Column(nullable = false)
	private Instant updatedAt;

	public PlatformIdentity(
			Platform platform,
			String platformUserId,
			String handle,
			String displayName,
			User user) {
		this.platform = platform;
		this.platformUserId = platformUserId;
		this.handle = handle;
		this.displayName = displayName;
		this.user = user;
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
}
