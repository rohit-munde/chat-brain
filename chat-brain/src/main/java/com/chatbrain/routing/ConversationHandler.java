package com.chatbrain.routing;

import com.chatbrain.events.UserResolvedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Order(300)
public class ConversationHandler implements MessageHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(ConversationHandler.class);

	@Override
	public boolean supports(UserResolvedEvent event) {
		return !event.getOriginalEvent().getMessage().isBlank();
	}

	@Override
	public Optional<String> handle(UserResolvedEvent event) {
		LOGGER.info("Conversation message received.");
		return Optional.empty();
	}
}
