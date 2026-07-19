package com.chatbrain.platform.youtube.metrics;

public enum YouTubeApiEndpoint {
	LIVE_BROADCASTS_LIST("liveBroadcasts.list"),
	LIVE_CHAT_MESSAGES_LIST("liveChatMessages.list"),
	LIVE_CHAT_MESSAGES_INSERT("liveChatMessages.insert"),
	CHANNELS_LIST("channels.list");

	private final String metricTag;

	YouTubeApiEndpoint(String metricTag) {
		this.metricTag = metricTag;
	}

	public String metricTag() {
		return metricTag;
	}
}
