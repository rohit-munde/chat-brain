package com.chatbrain.platform.youtube;

import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class YouTubePollingPolicyTests {

	private final YouTubePollingPolicy policy = new YouTubePollingPolicy(
			Duration.ofSeconds(10), Duration.ofSeconds(30));

	@Test
	void enforcesMinimumIntervalWhenYouTubeReturnsShorterDelay() {
		assertThat(policy.nextPollDelayMillis(100L)).isEqualTo(10_000L);
		assertThat(policy.nextPollDelayMillis(null)).isEqualTo(10_000L);
	}

	@Test
	void respectsLongerIntervalReturnedByYouTube() {
		assertThat(policy.nextPollDelayMillis(15_000L)).isEqualTo(15_000L);
		assertThat(policy.retryDelayMillis()).isEqualTo(30_000L);
	}
}
