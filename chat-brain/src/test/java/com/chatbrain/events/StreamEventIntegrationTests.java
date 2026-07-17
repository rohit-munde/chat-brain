package com.chatbrain.events;

import com.chatbrain.listeners.EventOrchestrationListener;
import com.chatbrain.platform.Platform;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class StreamEventIntegrationTests {

	@Test
	void springEventBusDeliversSubscriberEventToOrchestrationListener() {
		EventOrchestrationListener listener = mock(EventOrchestrationListener.class);
		try (AnnotationConfigApplicationContext context =
					new AnnotationConfigApplicationContext()) {
			context.registerBean(EventOrchestrationListener.class, () -> listener);
			context.refresh();
			SubscriberEvent event = subscriberEvent();

			context.publishEvent(event);

			verify(listener).onSubscriberEvent(event);
		}
	}

	@Test
	void chatMessagesRemainIndependentStreamEvents() {
		ChatMessageEvent event = new ChatMessageEvent(
				"YOUTUBE",
				"youtube-user-123",
				"@indieDeveloper",
				"Rohit Munde",
				"Hello",
				Instant.parse("2026-07-18T00:00:00Z"));

		assertThat(event).isInstanceOf(StreamEvent.class);
		assertThat(event.getEventType()).isEqualTo(EventType.CHAT_MESSAGE);
		assertThat(event.getMessage()).isEqualTo("Hello");
	}

	private SubscriberEvent subscriberEvent() {
		return new SubscriberEvent(
				Platform.YOUTUBE,
				"youtube-user-123",
				"Rohit Munde",
				null,
				Instant.parse("2026-07-18T00:00:00Z"));
	}
}
