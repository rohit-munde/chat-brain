package com.chatbrain.memory;

import com.chatbrain.events.ChatMessageEvent;

import java.util.List;

public interface MemoryRetriever {

	List<Memory> retrieve(ChatMessageEvent event);
}
