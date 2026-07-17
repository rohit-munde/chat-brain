package com.chatbrain.memory;

import com.chatbrain.entity.PlatformIdentity;
import com.chatbrain.events.ChatMessageEvent;
import com.chatbrain.platform.Platform;
import com.chatbrain.repository.PlatformIdentityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;

@Component
public class IdentityAwareMemoryRetriever implements MemoryRetriever {

	private static final Logger LOGGER = LoggerFactory.getLogger(IdentityAwareMemoryRetriever.class);

	private final PlatformIdentityRepository platformIdentityRepository;
	private final MemoryPersistenceService memoryPersistenceService;

	public IdentityAwareMemoryRetriever(
			PlatformIdentityRepository platformIdentityRepository,
			MemoryPersistenceService memoryPersistenceService) {
		this.platformIdentityRepository = platformIdentityRepository;
		this.memoryPersistenceService = memoryPersistenceService;
	}

	@Override
	public List<Memory> retrieve(ChatMessageEvent event) {
		Objects.requireNonNull(event, "event must not be null");
		Optional<PlatformIdentity> identity = platformIdentityRepository
				.findByPlatformAndPlatformUserId(
						parsePlatform(event.getPlatform()),
						requirePlatformUserId(event));

		if (identity.isEmpty()) {
			LOGGER.debug("No internal user was found for platform {} and user {}",
					event.getPlatform(), event.getPlatformUserId());
			return List.of();
		}

		LOGGER.debug("Resolved internal user {} for memory retrieval", identity.get().getUser().getId());
		List<Memory> memories = memoryPersistenceService.retrieveRecent(identity.get().getUser());
		LOGGER.info("Found {} memories", memories.size());
		return memories;
	}

	private Platform parsePlatform(String value) {
		try {
			return Platform.valueOf(value.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException exception) {
			return Platform.UNKNOWN;
		}
	}

	private String requirePlatformUserId(ChatMessageEvent event) {
		if (event.getPlatformUserId() == null || event.getPlatformUserId().isBlank()) {
			throw new IllegalArgumentException("ChatMessageEvent platformUserId must not be blank");
		}
		return event.getPlatformUserId();
	}
}
