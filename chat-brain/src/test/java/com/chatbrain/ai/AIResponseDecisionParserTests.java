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
				  "reply": "Hello Rohit!"
				}
				""");

		assertThat(decision.action()).isEqualTo(AIResponseAction.REPLY);
		assertThat(decision.reply()).isEqualTo("Hello Rohit!");
	}

	@Test
	void parsesIgnoreDecisionInsideMarkdownFence() {
		AIResponseDecision decision = parser.parse("""
				```json
				{"action":"IGNORE"}
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
	}

	@Test
	void fallsBackWhenReplyDecisionHasNoReply() {
		AIResponseDecision decision = parser.parse("""
				{"action":"REPLY","reply":null}
				""");

		assertThat(decision.action()).isEqualTo(AIResponseAction.REPLY);
		assertThat(decision.reply()).contains("\"action\":\"REPLY\"");
	}

	@Test
	void acceptsPreviousOptionalMetadataWithoutChangingV1Decision() {
		String output = """
				{"action":"IGNORE","remember":true,"reason":"Legacy metadata"}
				""";

		AIResponseDecision decision = parser.parse(output);

		assertThat(decision.action()).isEqualTo(AIResponseAction.IGNORE);
		assertThat(decision.reply()).isNull();
	}

	@Test
	void blankOutputUsesNonEmptySafeReplyFallback() {
		AIResponseDecision decision = parser.parse("   ");

		assertThat(decision.action()).isEqualTo(AIResponseAction.REPLY);
		assertThat(decision.reply()).isNotBlank();
	}
}
