package com.chatbrain.ai;

import com.chatbrain.events.ChatMessageEvent;

public interface AIResponseDecisionObserver {

	void onDecisionExecuted(ChatMessageEvent event, AIResponseDecision decision);
}
