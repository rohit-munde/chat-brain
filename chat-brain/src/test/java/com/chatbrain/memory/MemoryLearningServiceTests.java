package com.chatbrain.memory;

import com.chatbrain.entity.MemoryCategory;
import com.chatbrain.entity.MemorySource;
import com.chatbrain.entity.PlatformIdentity;
import com.chatbrain.entity.User;
import com.chatbrain.events.ChatMessageEvent;
import com.chatbrain.platform.Platform;
import com.chatbrain.repository.PlatformIdentityRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MemoryLearningServiceTests {

	@Test
	void skipsDuplicatesAndPersistsOnlyNewMemories() {
		MemoryExtractor extractor = mock(MemoryExtractor.class);
		PlatformIdentityRepository identityRepository = mock(PlatformIdentityRepository.class);
		MemoryPersistenceService persistenceService = mock(MemoryPersistenceService.class);
		ChatMessageEvent event = event("I like Java and I use Spring Boot");
		User user = User.create();
		PlatformIdentity identity = new PlatformIdentity(
				Platform.DISCORD, "user-123", "rohit", "Rohit", user);
		MemoryCandidate duplicate = new MemoryCandidate(MemoryCategory.PREFERENCE, "I like Java");
		MemoryCandidate newMemory = new MemoryCandidate(MemoryCategory.TECHNOLOGY, "I use Spring Boot");
		when(extractor.extract(event, "reply")).thenReturn(List.of(duplicate, newMemory));
		when(identityRepository.findByPlatformAndPlatformUserId(Platform.DISCORD, "user-123"))
				.thenReturn(Optional.of(identity));
		when(persistenceService.exists(user, duplicate.category(), duplicate.content()))
				.thenReturn(true);
		MemoryLearningService service = new MemoryLearningService(
				extractor, identityRepository, persistenceService);

		service.learn(event, "reply");

		verify(persistenceService, never()).persist(
				user, duplicate.category(), duplicate.content(), null,
				MemorySource.USER_MESSAGE);
		verify(persistenceService).persist(
				user, newMemory.category(), newMemory.content(), null,
				MemorySource.USER_MESSAGE);
	}

	@Test
	void extractionFailureIsContained() {
		MemoryExtractor extractor = mock(MemoryExtractor.class);
		PlatformIdentityRepository identityRepository = mock(PlatformIdentityRepository.class);
		MemoryPersistenceService persistenceService = mock(MemoryPersistenceService.class);
		ChatMessageEvent event = event("I like Java");
		when(extractor.extract(event, "reply"))
				.thenThrow(new IllegalStateException("extraction failed"));
		MemoryLearningService service = new MemoryLearningService(
				extractor, identityRepository, persistenceService);

		service.learn(event, "reply");

		verify(identityRepository, never()).findByPlatformAndPlatformUserId(
				Platform.DISCORD, "user-123");
	}

	private ChatMessageEvent event(String message) {
		return new ChatMessageEvent(
				"DISCORD",
				"user-123",
				"rohit",
				"Rohit",
				message,
				Instant.parse("2026-07-18T00:00:00Z"));
	}
}
