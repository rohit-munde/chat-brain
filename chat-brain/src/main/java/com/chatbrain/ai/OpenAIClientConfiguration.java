package com.chatbrain.ai;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(name = "communitybrain.ai.provider", havingValue = "openai")
public class OpenAIClientConfiguration {

	@Bean("openAIRestClient")
	RestClient openAIRestClient(
			@Value("${communitybrain.ai.openai.api-key}") String apiKey,
			@Value("${communitybrain.ai.openai.connect-timeout}") Duration connectTimeout,
			@Value("${communitybrain.ai.openai.read-timeout}") Duration readTimeout) {
		if (apiKey == null || apiKey.isBlank()) {
			throw new IllegalStateException(
					"OPENAI_API_KEY must be configured when the OpenAI provider is enabled");
		}

		HttpClient httpClient = HttpClient.newBuilder()
				.connectTimeout(connectTimeout)
				.build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(readTimeout);

		return RestClient.builder()
				.baseUrl("https://api.openai.com/v1")
				.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
				.requestFactory(requestFactory)
				.build();
	}
}
