package com.chatbrain.ai;

import com.chatbrain.events.ChatMessageEvent;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class PromptBuilder {

	public String build(ChatMessageEvent event) {
		Objects.requireNonNull(event, "event must not be null");
		return """
				Platform: %s
				Username: %s
				Display Name: %s
				Timestamp: %s

				Message:
				%s

				Respond as the AI co-host of the livestream.
				""".formatted(
				event.getPlatform(),
				event.getHandle(),
				event.getDisplayName(),
				event.getTimestamp(),
				event.getMessage());
	}
}
