package com.chatbrain.ai;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AIResponseDecisionParserTests {

	private final AIResponseDecisionParser parser =
			new AIResponseDecisionParser(new ObjectMapper());

	@Test
	void parsesReplyDecision() {
		AIResponseDecision decision = parser.parse("""
				{
				  "action": "REPLY",
				  "reply": "Hello Rohit!",
				  "remember": true,
				  "reason": "User introduced themselves"
				}
				""");

		assertThat(decision.action()).isEqualTo(AIResponseAction.REPLY);
		assertThat(decision.reply()).isEqualTo("Hello Rohit!");
		assertThat(decision.remember()).isTrue();
		assertThat(decision.reason()).isEqualTo("User introduced themselves");
	}

	@Test
	void parsesIgnoreDecisionInsideMarkdownFence() {
		AIResponseDecision decision = parser.parse("""
				```json
				{"action":"IGNORE","reply":null,"remember":false,"reason":"No action needed"}
				```
				""");

		assertThat(decision.action()).isEqualTo(AIResponseAction.IGNORE);
		assertThat(decision.reply()).isNull();
	}

	@Test
	void fallsBackToReplyForMalformedJson() {
		AIResponseDecision decision = parser.parse("Hello from the model");

		assertThat(decision.action()).isEqualTo(AIResponseAction.REPLY);
		assertThat(decision.reply()).isEqualTo("Hello from the model");
		assertThat(decision.remember()).isFalse();
	}

	@Test
	void fallsBackWhenReplyDecisionHasNoReply() {
		AIResponseDecision decision = parser.parse("""
				{"action":"REPLY","reply":null,"remember":false,"reason":"Invalid"}
				""");

		assertThat(decision.action()).isEqualTo(AIResponseAction.REPLY);
		assertThat(decision.reply()).contains("\"action\":\"REPLY\"");
	}

	@Test
	void fallsBackWhenRequiredRememberFieldIsMissing() {
		String output = """
				{"action":"IGNORE","reply":null,"reason":"Incomplete decision"}
				""";

		AIResponseDecision decision = parser.parse(output);

		assertThat(decision.action()).isEqualTo(AIResponseAction.REPLY);
		assertThat(decision.reply()).isEqualTo(output.trim());
	}
}
