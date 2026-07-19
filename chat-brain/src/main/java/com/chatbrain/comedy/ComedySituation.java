package com.chatbrain.comedy;

public record ComedySituation(
		ComedyOpportunity opportunity,
		String sourceText,
		String streamMood,
		boolean callbackAvailable) {
}
