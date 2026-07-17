package com.chatbrain;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ConfigurableApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

class ChatBrainApplicationTests {

	@Test
	void applicationStarts() {
		try (ConfigurableApplicationContext context = SpringApplication.run(
				ChatBrainApplication.class,
				"--spring.main.web-application-type=none",
				"--chatbrain.youtube.enabled=false",
				"--chatbrain.discord.enabled=false",
				"--communitybrain.ai.enabled=false")) {
			assertThat(context.isRunning()).isTrue();
		}
	}
}
