package com.chatbrain.ai;

public record AIResponseDecision(
		AIResponseAction action,
		String reply) {

	public AIResponseDecision {
		if (action == null) {
			throw new IllegalArgumentException("action must not be null");
		}
		if ((action == AIResponseAction.REPLY || action == AIResponseAction.COMMENT)
				&& (reply == null || reply.isBlank())) {
			throw new IllegalArgumentException("reply must not be blank when action publishes content");
		}
		if (reply != null) {
			reply = reply.trim();
		}
	}

	public static AIResponseDecision reply(String reply) {
		return new AIResponseDecision(AIResponseAction.REPLY, reply);
	}
}
