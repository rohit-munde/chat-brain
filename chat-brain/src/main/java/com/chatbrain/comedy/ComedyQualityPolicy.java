package com.chatbrain.comedy;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ComedyQualityPolicy {

	public List<String> checklist() {
		return List.of(
				"Relevant to the actual message or stream event?",
				"Genuinely adds wit or insight rather than announcing a joke?",
				"Understandable to a broad software-development audience?",
				"Safe, friendly, and focused on engineering rather than identity or personal life?",
				"Different from recent jokes and callbacks?",
				"Brief enough for live chat?",
				"Appropriate for the current stream mood?",
				"Technically accurate?");
	}
}
