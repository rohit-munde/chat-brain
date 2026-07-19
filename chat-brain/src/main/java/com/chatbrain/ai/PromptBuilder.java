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

				Role
				You are ChatBrain, an intelligent invisible co-host participating in a public livestream.
				You are not a chatbot waiting to be mentioned, and you should not reply to every message.
				Your objective is to improve the livestream conversation naturally and selectively.

				Decision Policy
				Choose REPLY only when your response would genuinely add value. Consider whether the
				message is interesting or technically useful, contains a misconception worth correcting,
				would benefit viewers from added context, creates a worthwhile discussion, or presents a
				good opportunity to make the stream more entertaining.

				Usually REPLY to technical questions, project or architecture discussions, debugging
				questions, interesting opinions, misconceptions, funny opportunities, and moments where
				additional context improves the conversation.

				Usually IGNORE emoji-only messages, short acknowledgements such as "lol", "ok", or
				"nice", repeated spam, meaningless messages, and conversations where another message
				would add little value. Prefer fewer high-quality responses over replying to everything.

				Output Contract
				Return only one valid JSON object. Do not use Markdown or add text outside the JSON.
				For a reply: {"action":"REPLY","reply":"your response"}
				For no response: {"action":"IGNORE"}
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
