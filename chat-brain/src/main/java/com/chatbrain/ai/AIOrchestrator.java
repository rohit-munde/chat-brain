package com.chatbrain.ai;

import com.chatbrain.events.ChatMessageEvent;
import com.chatbrain.memory.Memory;
import com.chatbrain.memory.MemoryLearningService;
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
	private final AIResponseDecisionParser decisionParser;
	private final YouTubePublisher youtubePublisher;
	private final MemoryLearningService memoryLearningService;
	private final List<AIResponseDecisionObserver> decisionObservers;

	public AIOrchestrator(
			MemoryRetriever memoryRetriever,
			PromptBuilder promptBuilder,
			LLMClient llmClient,
			AIResponseDecisionParser decisionParser,
			YouTubePublisher youtubePublisher,
			MemoryLearningService memoryLearningService,
			List<AIResponseDecisionObserver> decisionObservers) {
		this.memoryRetriever = memoryRetriever;
		this.promptBuilder = promptBuilder;
		this.llmClient = llmClient;
		this.decisionParser = decisionParser;
		this.youtubePublisher = youtubePublisher;
		this.memoryLearningService = memoryLearningService;
		this.decisionObservers = List.copyOf(decisionObservers);
	}

	public void process(ChatMessageEvent event) {
		Objects.requireNonNull(event, "event must not be null");
		LOGGER.info("Retrieving Memories");
		List<Memory> memories = List.copyOf(memoryRetriever.retrieve(event));
		LOGGER.info("Retrieved {} memories", memories.size());

		LOGGER.info("Building Prompt");
		String prompt = promptBuilder.build(event, memories);

		LOGGER.info("Calling LLM");
		String llmOutput = llmClient.generateReply(prompt);
		AIResponseDecision decision = decisionParser.parse(llmOutput);
		executeDecision(event, decision);
		try {
			memoryLearningService.learn(event, memoryLearningInput(decision, llmOutput));
		} catch (RuntimeException exception) {
			LOGGER.error("Memory learning failed after AI decision execution: {}",
					exception.getMessage(), exception);
		}
	}

	private void executeDecision(ChatMessageEvent event, AIResponseDecision decision) {
		LOGGER.info("Executing AI decision [action={}, remember={}, reason={}]",
				decision.action(), decision.remember(), decision.reason());
			switch (decision.action()) {
			case REPLY -> youtubePublisher.publish(decision.reply());
			case IGNORE -> LOGGER.info("AI decision ignored the message; no reply will be published");
		}
		for (AIResponseDecisionObserver observer : decisionObservers) {
			try {
				observer.onDecisionExecuted(event, decision);
			} catch (RuntimeException exception) {
				LOGGER.warn("AI decision observer failed: {}", exception.getMessage(), exception);
			}
		}
	}

	private String memoryLearningInput(AIResponseDecision decision, String llmOutput) {
		return decision.reply() == null ? llmOutput : decision.reply();
	}
}
