package com.chatbrain.platform;

public interface PlatformAdapter {

	Platform getPlatform();

	void start();

	void stop();
}
