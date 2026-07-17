package com.chatbrain.routing;

import com.chatbrain.events.UserResolvedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Order(200)
public class CommandHandler implements MessageHandler {

	private static final Logger LOGGER = LoggerFactory.getLogger(CommandHandler.class);

	@Override
	public boolean supports(UserResolvedEvent event) {
		return event.getOriginalEvent().getMessage().trim().startsWith("!");
	}

	@Override
	public Optional<String> handle(UserResolvedEvent event) {
		LOGGER.info("Unknown command");
		return Optional.empty();
	}
}
