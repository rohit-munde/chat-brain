package com.chatbrain.listeners;

import com.chatbrain.events.SubscriberEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class EventOrchestrationListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(EventOrchestrationListener.class);

	@EventListener
	public void onSubscriberEvent(SubscriberEvent event) {
		LOGGER.info("SubscriberEvent received");
		LOGGER.info(
				"Subscriber event awaiting AI decision [platform={}, platformUserId={}, "
						+ "displayName={}, handle={}, timestamp={}]",
				event.getPlatform(),
				event.getPlatformUserId(),
				event.getDisplayName(),
				event.getHandle(),
				event.getTimestamp());
	}
}
