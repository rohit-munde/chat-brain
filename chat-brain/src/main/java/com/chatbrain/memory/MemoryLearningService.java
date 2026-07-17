package com.chatbrain.memory;

import com.chatbrain.entity.MemorySource;
import com.chatbrain.entity.PlatformIdentity;
import com.chatbrain.events.ChatMessageEvent;
import com.chatbrain.platform.Platform;
import com.chatbrain.repository.PlatformIdentityRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
public class MemoryLearningService {

	private static final Logger LOGGER = LoggerFactory.getLogger(MemoryLearningService.class);

	private final MemoryExtractor memoryExtractor;
	private final PlatformIdentityRepository identityRepository;
	private final MemoryPersistenceService memoryPersistenceService;

	public MemoryLearningService(
			MemoryExtractor memoryExtractor,
			PlatformIdentityRepository identityRepository,
			MemoryPersistenceService memoryPersistenceService) {
		this.memoryExtractor = memoryExtractor;
		this.identityRepository = identityRepository;
		this.memoryPersistenceService = memoryPersistenceService;
	}

	public void learn(ChatMessageEvent event, String aiReply) {
		try {
			learnSafely(event, aiReply);
		} catch (RuntimeException exception) {
			LOGGER.error("Memory learning failed: {}", exception.getMessage(), exception);
		}
	}

	private void learnSafely(ChatMessageEvent event, String aiReply) {
		Objects.requireNonNull(event, "event must not be null");
		LOGGER.info("Memory Learning Started");
		List<MemoryCandidate> candidates = List.copyOf(memoryExtractor.extract(event, aiReply));
		LOGGER.info("{} candidate memories extracted", candidates.size());
		if (candidates.isEmpty()) {
			LOGGER.info("0 duplicate memories skipped");
			LOGGER.info("0 memories persisted");
			return;
		}

		PlatformIdentity identity = identityRepository.findByPlatformAndPlatformUserId(
				parsePlatform(event.getPlatform()),
				requirePlatformUserId(event))
				.orElseThrow(() -> new IllegalStateException(
						"Cannot learn memory because the platform identity was not resolved"));

		int duplicates = 0;
		int persisted = 0;
		Set<String> processedCandidates = new HashSet<>();
		for (MemoryCandidate candidate : candidates) {
			String candidateKey = candidate.category() + ":"
					+ candidate.content().toLowerCase(Locale.ROOT);
			if (!processedCandidates.add(candidateKey)
					|| memoryPersistenceService.exists(
					identity.getUser(), candidate.category(), candidate.content())) {
				duplicates++;
				continue;
			}
			memoryPersistenceService.persist(
					identity.getUser(),
					candidate.category(),
					candidate.content(),
					null,
					MemorySource.USER);
			persisted++;
		}

		LOGGER.info("{} duplicate memories skipped", duplicates);
		LOGGER.info("{} memories persisted", persisted);
	}

	private Platform parsePlatform(String value) {
		try {
			return Platform.valueOf(value.trim().toUpperCase(Locale.ROOT));
		} catch (IllegalArgumentException | NullPointerException exception) {
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
