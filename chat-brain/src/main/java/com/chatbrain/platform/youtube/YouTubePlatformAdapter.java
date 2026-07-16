package com.chatbrain.platform.youtube;

import com.chatbrain.platform.Platform;
import com.chatbrain.platform.PlatformAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class YouTubePlatformAdapter implements PlatformAdapter {

	private static final Logger LOGGER = LoggerFactory.getLogger(YouTubePlatformAdapter.class);

	@Override
	public Platform getPlatform() {
		return Platform.YOUTUBE;
	}

	@Override
	public void start() {
		LOGGER.info("Starting YouTube adapter...");
	}

	@Override
	public void stop() {
		LOGGER.info("Stopping YouTube adapter...");
	}
}
