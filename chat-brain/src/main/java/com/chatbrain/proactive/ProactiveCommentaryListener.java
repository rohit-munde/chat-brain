package com.chatbrain.proactive;

import com.chatbrain.events.ProactiveStreamEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ProactiveCommentaryListener {

	private final ProactiveCommentaryOrchestrator orchestrator;

	public ProactiveCommentaryListener(ProactiveCommentaryOrchestrator orchestrator) {
		this.orchestrator = orchestrator;
	}

	@EventListener
	public void onProactiveStreamEvent(ProactiveStreamEvent event) {
		orchestrator.process(event);
	}
}
