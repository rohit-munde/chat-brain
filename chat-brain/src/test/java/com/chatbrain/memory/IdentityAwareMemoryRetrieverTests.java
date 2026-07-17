package com.chatbrain.memory;

import com.chatbrain.entity.PlatformIdentity;
import com.chatbrain.entity.User;
import com.chatbrain.events.ChatMessageEvent;
import com.chatbrain.platform.Platform;
import com.chatbrain.repository.PlatformIdentityRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdentityAwareMemoryRetrieverTests {

	@Test
	void resolvesInternalIdentityWithoutReturningUnsafeGlobalMemories() {
		PlatformIdentityRepository identityRepository = mock(PlatformIdentityRepository.class);
		PlatformIdentity identity = new PlatformIdentity(
				Platform.DISCORD,
				"discord-user-123",
				"viewer",
				"Viewer",
				User.create());
		when(identityRepository.findByPlatformAndPlatformUserId(
				Platform.DISCORD,
				"discord-user-123"))
				.thenReturn(Optional.of(identity));
		MemoryRetriever retriever = new IdentityAwareMemoryRetriever(identityRepository);
		ChatMessageEvent event = new ChatMessageEvent(
				"DISCORD",
				"discord-user-123",
				"viewer",
				"Viewer",
				"Hello");

		assertThat(retriever.retrieve(event)).isEmpty();
		verify(identityRepository).findByPlatformAndPlatformUserId(
				Platform.DISCORD,
				"discord-user-123");
	}
}
