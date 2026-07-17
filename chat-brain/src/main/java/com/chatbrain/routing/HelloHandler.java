package com.chatbrain.routing;

import com.chatbrain.events.UserResolvedEvent;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Order(100)
public class HelloHandler implements MessageHandler {

	private static final String TRIGGER = "hello bot";
	private static final String RESPONSE = "Hello from ChatBrain 👋";

	@Override
	public boolean supports(UserResolvedEvent event) {
		return TRIGGER.equalsIgnoreCase(event.getOriginalEvent().getMessage().trim());
	}

	@Override
	public Optional<String> handle(UserResolvedEvent event) {
		return Optional.of(RESPONSE);
	}
}
