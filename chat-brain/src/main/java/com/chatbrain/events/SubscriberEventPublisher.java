package com.chatbrain.events;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class SubscriberEventPublisher {

	private static final Logger LOGGER = LoggerFactory.getLogger(SubscriberEventPublisher.class);

	private final ApplicationEventPublisher eventPublisher;
	private final boolean enabled;

	public SubscriberEventPublisher(
			ApplicationEventPublisher eventPublisher,
			@Value("${communitybrain.events.subscribers.enabled:true}") boolean enabled) {
		this.eventPublisher = eventPublisher;
		this.enabled = enabled;
	}

	public void publish(SubscriberEvent event) {
		Objects.requireNonNull(event, "event must not be null");
		LOGGER.info("Subscriber detected");
		if (!enabled) {
			LOGGER.debug("Subscriber event publishing is disabled");
			return;
		}

		LOGGER.info("Publishing SubscriberEvent");
		eventPublisher.publishEvent(event);
	}
}
