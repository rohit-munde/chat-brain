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
	private static final String RESPONSE_INSTRUCTION =
			"\n\nDecide whether the AI co-host should reply to this message.";

	@Override
	public String generateReply(String prompt) {
		int messageStart = prompt.indexOf(MESSAGE_PREFIX);
		int messageEnd = prompt.indexOf(RESPONSE_INSTRUCTION);
		if (messageStart < 0 || messageEnd < messageStart) {
			throw new IllegalArgumentException("Prompt does not contain the expected message section");
		}

		String message = prompt.substring(messageStart + MESSAGE_PREFIX.length(), messageEnd).trim();
		String reply = "AI Response: " + message;
		String escapedReply = new String(JsonStringEncoder.getInstance().quoteAsString(reply));
		return """
				{"action":"REPLY","reply":"%s","remember":false,"reason":"Fake provider response"}
				""".formatted(escapedReply).trim();
	}
}
