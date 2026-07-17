package com.chatbrain.memory;

import com.chatbrain.events.ChatMessageEvent;

import java.util.List;

public interface MemoryExtractor {

	List<MemoryCandidate> extract(ChatMessageEvent event, String aiReply);
}
