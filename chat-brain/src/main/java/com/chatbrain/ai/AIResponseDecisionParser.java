package com.chatbrain.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class AIResponseDecisionParser {

	private static final Logger LOGGER = LoggerFactory.getLogger(AIResponseDecisionParser.class);
	private static final String JSON_FENCE = "```json";
	private static final String GENERIC_FENCE = "```";
	private static final String EMPTY_OUTPUT_FALLBACK_REPLY =
			"I’m temporarily unable to generate a response. Please try again shortly.";

	private final ObjectMapper objectMapper;

	public AIResponseDecisionParser(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public AIResponseDecision parse(String llmOutput) {
		if (llmOutput == null || llmOutput.isBlank()) {
			LOGGER.warn("LLM returned no decision output; using the safe reply fallback");
			return AIResponseDecision.reply(EMPTY_OUTPUT_FALLBACK_REPLY);
		}

		try {
			DecisionPayload payload = objectMapper.readValue(
					removeMarkdownFence(llmOutput), DecisionPayload.class);
			return new AIResponseDecision(
					payload.action(),
					payload.reply());
		} catch (JsonProcessingException | IllegalArgumentException exception) {
			LOGGER.warn("Unable to parse structured AI decision; treating the complete output as a reply: {}",
					exception.getMessage());
			return AIResponseDecision.reply(llmOutput);
		}
	}

	private String removeMarkdownFence(String output) {
		String trimmedOutput = output.trim();
		if (trimmedOutput.startsWith(JSON_FENCE) && trimmedOutput.endsWith(GENERIC_FENCE)) {
			return trimmedOutput.substring(
					JSON_FENCE.length(),
					trimmedOutput.length() - GENERIC_FENCE.length()).trim();
		}
		if (trimmedOutput.startsWith(GENERIC_FENCE) && trimmedOutput.endsWith(GENERIC_FENCE)) {
			return trimmedOutput.substring(
					GENERIC_FENCE.length(),
					trimmedOutput.length() - GENERIC_FENCE.length()).trim();
		}
		return trimmedOutput;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record DecisionPayload(
			AIResponseAction action,
			String reply) {
	}
}
