package com.chatbrain.ai;

public record AIResponseDecision(
		AIResponseAction action,
		String reply,
		boolean remember,
		String reason) {

	public AIResponseDecision {
		if (action == null) {
			throw new IllegalArgumentException("action must not be null");
		}
		if (action == AIResponseAction.REPLY && (reply == null || reply.isBlank())) {
			throw new IllegalArgumentException("reply must not be blank when action is REPLY");
		}
		if (reply != null) {
			reply = reply.trim();
		}
		if (reason != null) {
			reason = reason.trim();
		}
	}

	public static AIResponseDecision reply(String reply) {
		return new AIResponseDecision(
				AIResponseAction.REPLY,
				reply,
				false,
				"LLM output was not a valid structured decision");
	}
}
