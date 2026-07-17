package com.chatbrain.platform;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PlatformReplyDispatcher {

	private static final Logger LOGGER = LoggerFactory.getLogger(PlatformReplyDispatcher.class);
	private final List<PlatformMessageSender> messageSenders;

	public PlatformReplyDispatcher(List<PlatformMessageSender> messageSenders) {
		this.messageSenders = List.copyOf(messageSenders);
	}

	public void dispatch(Platform platform, String message) {
		messageSenders.stream()
				.filter(sender -> sender.getPlatform() == platform)
				.findFirst()
				.ifPresentOrElse(
						sender -> sender.sendMessage(message),
						() -> LOGGER.warn("No message sender is available for platform {}", platform));
	}
}
