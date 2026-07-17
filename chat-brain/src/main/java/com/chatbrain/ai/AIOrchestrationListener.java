package com.chatbrain.ai;

import com.chatbrain.events.ChatMessageEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.event.EventListener;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "communitybrain.ai.enabled", havingValue = "true")
public class AIOrchestrationListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(AIOrchestrationListener.class);

	private final AIOrchestrator orchestrator;

	public AIOrchestrationListener(AIOrchestrator orchestrator) {
		this.orchestrator = orchestrator;
	}

	@EventListener
	@Order(Ordered.LOWEST_PRECEDENCE)
	public void onChatMessage(ChatMessageEvent event) {
		LOGGER.info("Received ChatMessageEvent");
		orchestrator.process(event);
	}
}
