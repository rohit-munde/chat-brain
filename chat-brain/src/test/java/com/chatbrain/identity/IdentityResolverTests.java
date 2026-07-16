package com.chatbrain.identity;

import com.chatbrain.entity.PlatformIdentity;
import com.chatbrain.entity.User;
import com.chatbrain.events.ChatMessageEvent;
import com.chatbrain.events.UserResolvedEvent;
import com.chatbrain.platform.Platform;
import com.chatbrain.repository.PlatformIdentityRepository;
import com.chatbrain.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class IdentityResolverTests {

	private UserRepository userRepository;
	private PlatformIdentityRepository platformIdentityRepository;
	private ApplicationEventPublisher eventPublisher;
	private IdentityResolver identityResolver;

	@BeforeEach
	void setUp() {
		userRepository = mock(UserRepository.class);
		platformIdentityRepository = mock(PlatformIdentityRepository.class);
		eventPublisher = mock(ApplicationEventPublisher.class);
		identityResolver = new IdentityResolver(
				userRepository,
				platformIdentityRepository,
				eventPublisher);
	}

	@Test
	void createsUserAndPlatformIdentityForFirstMessage() {
		ChatMessageEvent message = message("First Name");
		when(platformIdentityRepository.findByPlatformAndChannelId(
				Platform.YOUTUBE,
				"channel-123"))
				.thenReturn(Optional.empty());
		when(userRepository.save(any(User.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		when(platformIdentityRepository.save(any(PlatformIdentity.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));

		User resolvedUser = identityResolver.resolve(message);

		assertThat(resolvedUser.getRelationshipScore()).isEqualTo(1);
		assertThat(resolvedUser.getFirstSeen()).isEqualTo(message.getTimestamp());
		assertThat(resolvedUser.getLastSeen()).isEqualTo(message.getTimestamp());
		ArgumentCaptor<PlatformIdentity> identityCaptor =
				ArgumentCaptor.forClass(PlatformIdentity.class);
		verify(platformIdentityRepository).save(identityCaptor.capture());
		assertThat(identityCaptor.getValue().getPlatform()).isEqualTo(Platform.YOUTUBE);
		assertThat(identityCaptor.getValue().getChannelId()).isEqualTo("channel-123");
		assertThat(identityCaptor.getValue().getVisibleName()).isEqualTo("First Name");
		assertThat(resolvedUser.getRealName()).isNull();
		verify(eventPublisher).publishEvent(any(UserResolvedEvent.class));
	}

	@Test
	void updatesExistingIdentityWithoutCreatingAnotherUser() {
		User existingUser = User.create();
		existingUser.setRelationshipScore(1);
		PlatformIdentity existingIdentity = new PlatformIdentity(
				Platform.YOUTUBE,
				"channel-123",
				"Old Name",
				existingUser);
		ChatMessageEvent message = message("Updated Name");
		when(platformIdentityRepository.findByPlatformAndChannelId(
				Platform.YOUTUBE,
				"channel-123"))
				.thenReturn(Optional.of(existingIdentity));
		when(platformIdentityRepository.save(existingIdentity)).thenReturn(existingIdentity);

		User resolvedUser = identityResolver.resolve(message);

		assertThat(resolvedUser).isSameAs(existingUser);
		assertThat(resolvedUser.getRelationshipScore()).isEqualTo(2);
		assertThat(resolvedUser.getLastSeen()).isEqualTo(message.getTimestamp());
		assertThat(existingIdentity.getVisibleName()).isEqualTo("Updated Name");
		assertThat(resolvedUser.getRealName()).isNull();
		verify(userRepository, never()).save(any(User.class));
		verify(eventPublisher).publishEvent(any(UserResolvedEvent.class));
	}

	private ChatMessageEvent message(String visibleName) {
		return new ChatMessageEvent(
				"YOUTUBE",
				"channel-123",
				visibleName,
				"Hello ChatBrain");
	}
}
