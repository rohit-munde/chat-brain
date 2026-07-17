package com.chatbrain.routing;

import com.chatbrain.entity.PlatformIdentity;
import com.chatbrain.entity.User;
import com.chatbrain.events.ChatMessageEvent;
import com.chatbrain.events.UserResolvedEvent;
import com.chatbrain.platform.Platform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MessageRouterTests {

	private MessageRouter messageRouter;

	@BeforeEach
	void setUp() {
		messageRouter = new MessageRouter(List.of(
				new HelloHandler(),
				new CommandHandler(),
				new ConversationHandler(),
				new IgnoreHandler()));
	}

	@Test
	void routesHelloMessageToHelloHandler() {
		assertThat(messageRouter.route(event("  HeLLo BoT  ")))
				.contains("Hello from ChatBrain 👋");
	}

	@Test
	void routesCommandWithoutProducingReply() {
		assertThat(messageRouter.route(event("!unknown"))).isEmpty();
	}

	@Test
	void routesConversationWithoutProducingReply() {
		assertThat(messageRouter.route(event("How are you?"))).isEmpty();
	}

	@Test
	void ignoresBlankMessage() {
		assertThat(messageRouter.route(event("   "))).isEmpty();
	}

	private UserResolvedEvent event(String message) {
		User user = User.create();
		PlatformIdentity identity = new PlatformIdentity(
				Platform.YOUTUBE,
				"channel-123",
				"@viewer",
				"Viewer",
				user);
		ChatMessageEvent chatMessageEvent = new ChatMessageEvent(
				"YOUTUBE",
				"channel-123",
				"@viewer",
				"Viewer",
				message);
		return new UserResolvedEvent(user, identity, chatMessageEvent);
	}
}
