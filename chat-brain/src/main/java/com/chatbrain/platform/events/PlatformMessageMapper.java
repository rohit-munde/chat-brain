package com.chatbrain.platform.events;

import com.chatbrain.events.ChatMessageEvent;
import com.chatbrain.platform.PlatformMessage;
import org.springframework.stereotype.Component;

@Component
public class PlatformMessageMapper {

	public ChatMessageEvent toChatMessageEvent(PlatformMessage message) {
		return new ChatMessageEvent(
				message.platform().name(),
				message.platformUserId(),
				message.handle(),
				message.displayName(),
				message.message());
	}
}
