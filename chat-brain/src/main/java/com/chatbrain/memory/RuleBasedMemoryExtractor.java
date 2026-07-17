package com.chatbrain.memory;

import com.chatbrain.entity.MemoryCategory;
import com.chatbrain.events.ChatMessageEvent;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class RuleBasedMemoryExtractor implements MemoryExtractor {

	private static final String NEXT_FACT =
			"my name is|i am|i'm|i work at|i use|i like|i prefer|my favorite|"
					+ "i'm building|i am building|i'm learning|i am learning|i live in";
	private static final String FACT_END =
			"(?=[.!?](?:\\s|$)|\\s+and\\s+(?=(?:" + NEXT_FACT + "))|$)";
	private static final List<ExtractionRule> RULES = List.of(
			rule(MemoryCategory.IDENTITY, "my\\s+name\\s+is"),
			rule(MemoryCategory.PROJECT, "i(?:'m|\\s+am)\\s+building"),
			rule(MemoryCategory.TECHNOLOGY, "i(?:'m|\\s+am)\\s+learning"),
			rule(MemoryCategory.GENERAL, "i\\s+work\\s+at"),
			rule(MemoryCategory.TECHNOLOGY, "i\\s+use"),
			rule(MemoryCategory.PREFERENCE, "i\\s+like"),
			rule(MemoryCategory.PREFERENCE, "i\\s+prefer"),
			rule(MemoryCategory.PREFERENCE, "my\\s+favorite"),
			rule(MemoryCategory.LOCATION, "i\\s+live\\s+in"),
			rule(MemoryCategory.IDENTITY,
					"i(?:'m|\\s+am)(?!\\s+(?:building|learning))"));

	@Override
	public List<MemoryCandidate> extract(ChatMessageEvent event, String aiReply) {
		Objects.requireNonNull(event, "event must not be null");
		String message = event.getMessage();
		if (message == null || message.isBlank()) {
			return List.of();
		}

		List<MemoryCandidate> candidates = new ArrayList<>();
		for (ExtractionRule rule : RULES) {
			Matcher matcher = rule.pattern().matcher(message);
			while (matcher.find()) {
				candidates.add(new MemoryCandidate(rule.category(), matcher.group().trim()));
			}
		}
		return List.copyOf(candidates);
	}

	private static ExtractionRule rule(MemoryCategory category, String phrase) {
		Pattern pattern = Pattern.compile(
				"(?i)\\b" + phrase + "\\s+.+?" + FACT_END);
		return new ExtractionRule(category, pattern);
	}

	private record ExtractionRule(MemoryCategory category, Pattern pattern) {
	}
}
