package com.chatbrain.routing;

import com.chatbrain.events.UserResolvedEvent;

import java.util.Optional;

public interface MessageHandler {

	boolean supports(UserResolvedEvent event);

	Optional<String> handle(UserResolvedEvent event);
}
