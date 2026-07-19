package com.chatbrain.ai;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import jakarta.annotation.PostConstruct;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Component
@ConditionalOnProperty(name = "communitybrain.ai.provider", havingValue = "openai")
public class OpenAILLMClient implements LLMClient {

	private static final Logger LOGGER = LoggerFactory.getLogger(OpenAILLMClient.class);
	private static final String FALLBACK_RESPONSE =
			"I’m temporarily unable to generate a response. Please try again shortly.";

	private final RestClient restClient;
	private final String model;

	public OpenAILLMClient(
			@Qualifier("openAIRestClient") RestClient restClient,
			@Value("${communitybrain.ai.openai.model}") String model) {
		this.restClient = restClient;
		if (model == null || model.isBlank()) {
			throw new IllegalArgumentException("OpenAI model must not be blank");
		}
		this.model = model;
	}

	@PostConstruct
	void logActiveProvider() {
		LOGGER.info("""
				====================================
				AI Provider: OpenAI
				Model: {}
				API Key: configured
				====================================""", model);
	}

	@Override
	public String generateReply(String prompt) {
		if (prompt == null || prompt.isBlank()) {
			throw new IllegalArgumentException("prompt must not be blank");
		}

		long startedAt = System.nanoTime();
		LOGGER.info("Calling OpenAI...");
		try {
			OpenAIResponse response = restClient.post()
					.uri("/responses")
					.body(new OpenAIRequest(model, prompt))
					.retrieve()
					.body(OpenAIResponse.class);
			String reply = extractReply(response);
			LOGGER.info("OpenAI response received.");
			return reply;
		} catch (RuntimeException exception) {
			LOGGER.error("OpenAI request failed: {}", exception.getMessage(), exception);
			return FALLBACK_RESPONSE;
		} finally {
			long latencyMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - startedAt);
			LOGGER.info("Latency: {} ms", latencyMillis);
		}
	}

	private String extractReply(OpenAIResponse response) {
		if (response == null || response.output() == null) {
			throw new IllegalStateException("OpenAI returned no output");
		}
		return response.output().stream()
				.filter(Objects::nonNull)
				.map(OpenAIOutput::content)
				.filter(Objects::nonNull)
				.flatMap(List::stream)
				.filter(Objects::nonNull)
				.filter(content -> "output_text".equals(content.type()))
				.map(OpenAIContent::text)
				.filter(text -> text != null && !text.isBlank())
				.findFirst()
				.orElseThrow(() -> new IllegalStateException("OpenAI returned no text response"));
	}

	private record OpenAIRequest(String model, String input) {
	}

	private record OpenAIResponse(List<OpenAIOutput> output) {
	}

	private record OpenAIOutput(List<OpenAIContent> content) {
	}

	private record OpenAIContent(String type, String text) {
	}
}
