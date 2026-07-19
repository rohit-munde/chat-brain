package com.chatbrain.platform.youtube.metrics;

import java.util.Map;

public record YouTubeApiMetrics(
		Map<YouTubeApiEndpoint, EndpointMetrics> endpoints,
		long currentPollingIntervalMillis,
		long messagesReceived,
		long messagesIgnored,
		long repliesPublished,
		long broadcastDiscoveries,
		long uniqueAuthorEnrichments,
		long authorCacheHits,
		long authorCacheMisses) {

	public YouTubeApiMetrics {
		endpoints = Map.copyOf(endpoints);
	}

	public record EndpointMetrics(
			long requests,
			long successes,
			long failures,
			double averageLatencyMillis,
			double totalLatencyMillis) {
	}
}
