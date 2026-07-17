package com.chatbrain.ai;

import com.chatbrain.events.ChatMessageEvent;
import com.chatbrain.memory.Memory;
import com.chatbrain.memory.MemoryRetriever;
import com.chatbrain.platform.youtube.YouTubePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Objects;

@Service
public class AIOrchestrator {

	private static final Logger LOGGER = LoggerFactory.getLogger(AIOrchestrator.class);

	private final MemoryRetriever memoryRetriever;
	private final PromptBuilder promptBuilder;
	private final LLMClient llmClient;
	private final YouTubePublisher youtubePublisher;

	public AIOrchestrator(
			MemoryRetriever memoryRetriever,
			PromptBuilder promptBuilder,
			LLMClient llmClient,
			YouTubePublisher youtubePublisher) {
		this.memoryRetriever = memoryRetriever;
		this.promptBuilder = promptBuilder;
		this.llmClient = llmClient;
		this.youtubePublisher = youtubePublisher;
	}

	public void process(ChatMessageEvent event) {
		Objects.requireNonNull(event, "event must not be null");
		LOGGER.info("Retrieving Memories");
		List<Memory> memories = List.copyOf(memoryRetriever.retrieve(event));
		LOGGER.info("Retrieved {} memories", memories.size());

		LOGGER.info("Building Prompt");
		String prompt = promptBuilder.build(event, memories);

		LOGGER.info("Calling LLM");
		String response = llmClient.generateReply(prompt);
		youtubePublisher.publish(response);
	}
}
