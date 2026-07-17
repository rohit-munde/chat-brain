package com.chatbrain.ai;

import com.chatbrain.events.ChatMessageEvent;
import com.chatbrain.memory.Memory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class PromptBuilder {

	public String build(ChatMessageEvent event, List<Memory> memories) {
		Objects.requireNonNull(event, "event must not be null");
		List<Memory> safeMemories = List.copyOf(
				Objects.requireNonNull(memories, "memories must not be null"));
		return """
				Platform: %s
				Username: %s
				Display Name: %s
				Timestamp: %s

				Relevant Memories
				%s

				Current Message:
				%s

				Respond as the AI co-host of the livestream.
				""".formatted(
				event.getPlatform(),
				event.getHandle(),
				event.getDisplayName(),
				event.getTimestamp(),
				formatMemories(safeMemories),
				event.getMessage());
	}

	private String formatMemories(List<Memory> memories) {
		if (memories.isEmpty()) {
			return "- None";
		}
		return memories.stream()
				.map(memory -> "- [%s] %s".formatted(memory.category(), memory.content()))
				.collect(Collectors.joining("\n"));
	}
}
