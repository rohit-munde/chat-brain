package com.chatbrain.events;

import com.chatbrain.platform.Platform;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.time.Instant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class SubscriberEventPublisherTests {

	@Test
	void publishesSubscriberEventWhenEnabled() {
		ApplicationEventPublisher applicationEventPublisher =
				mock(ApplicationEventPublisher.class);
		SubscriberEventPublisher publisher =
				new SubscriberEventPublisher(applicationEventPublisher, true);
		SubscriberEvent event = subscriberEvent();

		publisher.publish(event);

		verify(applicationEventPublisher).publishEvent(event);
	}

	@Test
	void doesNotPublishSubscriberEventWhenDisabled() {
		ApplicationEventPublisher applicationEventPublisher =
				mock(ApplicationEventPublisher.class);
		SubscriberEventPublisher publisher =
				new SubscriberEventPublisher(applicationEventPublisher, false);
		SubscriberEvent event = subscriberEvent();

		publisher.publish(event);

		verify(applicationEventPublisher, never()).publishEvent(event);
	}

	private SubscriberEvent subscriberEvent() {
		return new SubscriberEvent(
				Platform.YOUTUBE,
				"youtube-user-123",
				"Rohit Munde",
				"@indieDeveloper",
				Instant.parse("2026-07-18T00:00:00Z"));
	}
}
