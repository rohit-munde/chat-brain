package com.chatbrain.routing;

import com.chatbrain.events.UserResolvedEvent;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class IgnoreHandler implements MessageHandler {

	@Override
	public boolean supports(UserResolvedEvent event) {
		return true;
	}

	@Override
	public Optional<String> handle(UserResolvedEvent event) {
		return Optional.empty();
	}
}
