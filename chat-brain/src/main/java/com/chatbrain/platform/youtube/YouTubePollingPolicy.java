package com.chatbrain.platform.youtube;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConditionalOnProperty(name = "chatbrain.youtube.enabled", havingValue = "true")
public class YouTubePollingPolicy {

	private final long minimumPollIntervalMillis;
	private final long retryIntervalMillis;

	public YouTubePollingPolicy(
			@Value("${chatbrain.youtube.minimum-poll-interval:10s}") Duration minimumPollInterval,
			@Value("${chatbrain.youtube.retry-interval:30s}") Duration retryInterval) {
		this.minimumPollIntervalMillis = requirePositive(
				minimumPollInterval, "minimum poll interval");
		this.retryIntervalMillis = requirePositive(retryInterval, "retry interval");
	}

	public long nextPollDelayMillis(Long youtubePollingIntervalMillis) {
		if (youtubePollingIntervalMillis == null || youtubePollingIntervalMillis <= 0) {
			return minimumPollIntervalMillis;
		}
		return Math.max(youtubePollingIntervalMillis, minimumPollIntervalMillis);
	}

	public long retryDelayMillis() {
		return retryIntervalMillis;
	}

	private long requirePositive(Duration duration, String propertyName) {
		if (duration == null || duration.isZero() || duration.isNegative()) {
			throw new IllegalArgumentException(propertyName + " must be greater than zero");
		}
		return duration.toMillis();
	}
}
