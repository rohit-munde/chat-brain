package com.chatbrain.ai;

import com.fasterxml.jackson.core.io.JsonStringEncoder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
		name = "communitybrain.ai.provider",
		havingValue = "fake",
		matchIfMissing = true)
public class FakeLLMClient implements LLMClient {

	private static final String MESSAGE_PREFIX = "Current Message:\n";
	private static final String EVENT_SUMMARY_PREFIX = "Summary: ";
	private static final String RESPONSE_INSTRUCTION = "\n\nOutput Contract\n";

	@Override
	public String generateReply(String prompt) {
		if (prompt.contains("Current Event\n")) {
			return proactiveDecision(prompt);
		}
		int messageStart = prompt.indexOf(MESSAGE_PREFIX);
		int messageEnd = prompt.indexOf(RESPONSE_INSTRUCTION);
		if (messageStart < 0 || messageEnd < messageStart) {
			throw new IllegalArgumentException("Prompt does not contain the expected message section");
		}

		String message = prompt.substring(messageStart + MESSAGE_PREFIX.length(), messageEnd).trim();
		String reply = "AI Response: " + message;
		String escapedReply = new String(JsonStringEncoder.getInstance().quoteAsString(reply));
		return """
				{"action":"REPLY","reply":"%s"}
				""".formatted(escapedReply).trim();
	}

	private String proactiveDecision(String prompt) {
		int summaryStart = prompt.indexOf(EVENT_SUMMARY_PREFIX, prompt.indexOf("Current Event\n"));
		int summaryEnd = prompt.indexOf(RESPONSE_INSTRUCTION, summaryStart);
		if (summaryStart < 0 || summaryEnd < summaryStart) {
			throw new IllegalArgumentException("Prompt does not contain the expected event section");
		}
		String summary = prompt.substring(summaryStart + EVENT_SUMMARY_PREFIX.length(), summaryEnd).trim();
		String escapedReply = new String(JsonStringEncoder.getInstance()
				.quoteAsString("AI Comment: " + summary));
		return """
				{"action":"COMMENT","reply":"%s"}
				""".formatted(escapedReply).trim();
	}
}
