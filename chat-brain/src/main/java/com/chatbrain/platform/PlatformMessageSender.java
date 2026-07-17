package com.chatbrain.platform;

public interface PlatformMessageSender {

	Platform getPlatform();

	void sendMessage(String message);
}
