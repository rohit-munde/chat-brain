package com.chatbrain.listeners;

import com.chatbrain.events.ChatMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ChatMessageListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(ChatMessageListener.class);

	@EventListener
	public void onChatMessage(ChatMessageEvent event) {
		LOGGER.info("""
				----------------------------------
				Platform         : {}
				Platform User ID : {}
				Handle           : {}
				Display Name     : {}
				Message          : {}
				Timestamp        : {}
				----------------------------------""",
				event.getPlatform(),
				event.getPlatformUserId(),
				event.getHandle(),
				event.getDisplayName(),
				event.getMessage(),
				event.getTimestamp());
	}
}
