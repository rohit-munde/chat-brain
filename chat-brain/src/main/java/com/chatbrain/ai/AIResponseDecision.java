package com.chatbrain.ai;

public record AIResponseDecision(
		AIResponseAction action,
		String reply) {

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
	}

	public static AIResponseDecision reply(String reply) {
		return new AIResponseDecision(AIResponseAction.REPLY, reply);
	}
}
