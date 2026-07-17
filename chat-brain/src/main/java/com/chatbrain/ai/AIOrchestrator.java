package com.chatbrain.ai;

import com.chatbrain.events.ChatMessageEvent;
import com.chatbrain.platform.youtube.YouTubePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class AIOrchestrator {

	private static final Logger LOGGER = LoggerFactory.getLogger(AIOrchestrator.class);

	private final PromptBuilder promptBuilder;
	private final LLMClient llmClient;
	private final YouTubePublisher youtubePublisher;

	public AIOrchestrator(
			PromptBuilder promptBuilder,
			LLMClient llmClient,
			YouTubePublisher youtubePublisher) {
		this.promptBuilder = promptBuilder;
		this.llmClient = llmClient;
		this.youtubePublisher = youtubePublisher;
	}

	public void process(ChatMessageEvent event) {
		Objects.requireNonNull(event, "event must not be null");
		LOGGER.info("Building Prompt");
		String prompt = promptBuilder.build(event);

		LOGGER.info("Generating AI Response");
		String response = llmClient.generateReply(prompt);
		youtubePublisher.publish(response);
	}
}
