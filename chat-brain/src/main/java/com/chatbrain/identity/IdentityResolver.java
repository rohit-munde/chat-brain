package com.chatbrain.identity;

import com.chatbrain.entity.PlatformIdentity;
import com.chatbrain.entity.User;
import com.chatbrain.events.ChatMessageEvent;
import com.chatbrain.events.UserResolvedEvent;
import com.chatbrain.platform.Platform;
import com.chatbrain.repository.PlatformIdentityRepository;
import com.chatbrain.repository.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Component
public class IdentityResolver {

	private final UserRepository userRepository;
	private final PlatformIdentityRepository platformIdentityRepository;
	private final ApplicationEventPublisher eventPublisher;

	public IdentityResolver(
			UserRepository userRepository,
			PlatformIdentityRepository platformIdentityRepository,
			ApplicationEventPublisher eventPublisher) {
		this.userRepository = userRepository;
		this.platformIdentityRepository = platformIdentityRepository;
		this.eventPublisher = eventPublisher;
	}

	@EventListener
	@Transactional
	public User resolve(ChatMessageEvent event) {
		Platform platform = parsePlatform(event.getPlatform());
		String platformUserId = requirePlatformUserId(event);

		PlatformIdentity identity = platformIdentityRepository
				.findByPlatformAndPlatformUserId(platform, platformUserId)
				.map(existingIdentity -> updateExistingIdentity(existingIdentity, event))
				.orElseGet(() -> createIdentity(platform, platformUserId, event));

		eventPublisher.publishEvent(new UserResolvedEvent(identity.getUser(), identity, event));
		return identity.getUser();
	}

	private PlatformIdentity updateExistingIdentity(
			PlatformIdentity identity,
			ChatMessageEvent event) {
		User user = identity.getUser();
		user.setLastSeen(event.getTimestamp());
		user.setRelationshipScore(currentScore(user) + 1);
		identity.setHandle(event.getHandle());
		identity.setDisplayName(event.getDisplayName());
		return platformIdentityRepository.save(identity);
	}

	private PlatformIdentity createIdentity(
			Platform platform,
			String platformUserId,
			ChatMessageEvent event) {
		User user = User.create();
		user.setRelationshipScore(1);
		user.setFirstSeen(event.getTimestamp());
		user.setLastSeen(event.getTimestamp());
		User savedUser = userRepository.save(user);

		PlatformIdentity identity = new PlatformIdentity(
				platform,
				platformUserId,
				event.getHandle(),
				event.getDisplayName(),
				savedUser);
		return platformIdentityRepository.save(identity);
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

	private int currentScore(User user) {
		return user.getRelationshipScore() == null ? 0 : user.getRelationshipScore();
	}
}
