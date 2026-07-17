package com.chatbrain.memory;

import com.chatbrain.entity.PlatformIdentity;
import com.chatbrain.entity.User;
import com.chatbrain.events.ChatMessageEvent;
import com.chatbrain.platform.Platform;
import com.chatbrain.repository.PlatformIdentityRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdentityAwareMemoryRetrieverTests {

	@Test
	void retrievesOnlyMemoriesForTheResolvedInternalIdentity() {
		PlatformIdentityRepository identityRepository = mock(PlatformIdentityRepository.class);
		MemoryPersistenceService memoryPersistenceService = mock(MemoryPersistenceService.class);
		User user = User.create();
		user.setId(UUID.randomUUID());
		PlatformIdentity identity = new PlatformIdentity(
				Platform.DISCORD,
				"discord-user-123",
				"viewer",
				"Viewer",
				user);
		List<Memory> expectedMemories = List.of(new Memory(
				"PREFERENCE",
				"Likes Java",
				Instant.parse("2026-07-18T00:00:00Z")));
		when(identityRepository.findByPlatformAndPlatformUserId(
				Platform.DISCORD,
				"discord-user-123"))
				.thenReturn(Optional.of(identity));
		when(memoryPersistenceService.retrieveRecent(user)).thenReturn(expectedMemories);
		MemoryRetriever retriever = new IdentityAwareMemoryRetriever(
				identityRepository,
				memoryPersistenceService);
		ChatMessageEvent event = new ChatMessageEvent(
				"DISCORD",
				"discord-user-123",
				"viewer",
				"Viewer",
				"Hello");

		assertThat(retriever.retrieve(event)).containsExactlyElementsOf(expectedMemories);
		verify(identityRepository).findByPlatformAndPlatformUserId(
				Platform.DISCORD,
				"discord-user-123");
		verify(memoryPersistenceService).retrieveRecent(user);
	}
}
