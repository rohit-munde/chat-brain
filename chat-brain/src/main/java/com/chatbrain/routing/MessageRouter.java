package com.chatbrain.routing;

import com.chatbrain.events.UserResolvedEvent;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class MessageRouter {

	private final List<MessageHandler> handlers;

	public MessageRouter(List<MessageHandler> handlers) {
		this.handlers = List.copyOf(handlers);
	}

	public Optional<String> route(UserResolvedEvent event) {
		return handlers.stream()
				.filter(handler -> handler.supports(event))
				.findFirst()
				.flatMap(handler -> handler.handle(event));
	}
}
