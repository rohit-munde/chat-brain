package com.chatbrain.ai;

import org.springframework.stereotype.Component;

@Component
public class FakeLLMClient implements LLMClient {

	private static final String MESSAGE_PREFIX = "Message:\n";
	private static final String RESPONSE_INSTRUCTION = "\n\nRespond as the AI co-host of the livestream.";

	@Override
	public String generateReply(String prompt) {
		int messageStart = prompt.indexOf(MESSAGE_PREFIX);
		int messageEnd = prompt.indexOf(RESPONSE_INSTRUCTION);
		if (messageStart < 0 || messageEnd < messageStart) {
			throw new IllegalArgumentException("Prompt does not contain the expected message section");
		}

		String message = prompt.substring(messageStart + MESSAGE_PREFIX.length(), messageEnd).trim();
		return "AI Response: " + message;
	}
}
