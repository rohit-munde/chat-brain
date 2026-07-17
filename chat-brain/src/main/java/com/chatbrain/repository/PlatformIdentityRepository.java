package com.chatbrain.repository;

import com.chatbrain.entity.PlatformIdentity;
import com.chatbrain.platform.Platform;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface PlatformIdentityRepository extends JpaRepository<PlatformIdentity, UUID> {

	Optional<PlatformIdentity> findByPlatformAndPlatformUserId(
			Platform platform,
			String platformUserId);
}
