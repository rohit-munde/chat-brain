package com.chatbrain.memory;

import com.chatbrain.events.StreamEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class MemoryListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(MemoryListener.class);

	@EventListener
	public void onStreamEvent(StreamEvent event) {
		LOGGER.info("Received {}.", event.getClass().getSimpleName());
		LOGGER.info("Memory extraction not implemented.");
	}
}
