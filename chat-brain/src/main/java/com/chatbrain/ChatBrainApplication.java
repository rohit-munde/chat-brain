package com.chatbrain;

import com.chatbrain.events.ChatMessageEvent;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class ChatBrainApplication {

	public static void main(String[] args) {
		SpringApplication.run(ChatBrainApplication.class, args);
	}

	@Bean
	ApplicationRunner publishStartupChatMessage(ApplicationEventPublisher eventPublisher) {
		return args -> eventPublisher.publishEvent(
				new ChatMessageEvent("YouTube", "Rohit", "Hello ChatBrain 🚀"));
	}
}
