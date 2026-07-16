package com.chatbrain.repository;

import com.chatbrain.entity.Alias;
import com.chatbrain.entity.Platform;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AliasRepository extends JpaRepository<Alias, UUID> {

	Optional<Alias> findByPlatformAndUsername(Platform platform, String username);
}
