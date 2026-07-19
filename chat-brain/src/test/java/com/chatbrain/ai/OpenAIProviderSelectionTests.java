package com.chatbrain.ai;

import org.junit.jupiter.api.Test;
import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class OpenAIProviderSelectionTests {

	private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
			.withInitializer(context -> context.getBeanFactory().setConversionService(
					ApplicationConversionService.getSharedInstance()))
			.withUserConfiguration(
					OpenAIClientConfiguration.class,
					OpenAILLMClient.class,
					FakeLLMClient.class)
			.withPropertyValues(
					"communitybrain.ai.openai.model=gpt-5.5",
					"communitybrain.ai.openai.connect-timeout=10s",
					"communitybrain.ai.openai.read-timeout=60s");

	@Test
	void selectsOnlyOpenAIClientWhenOpenAIProviderIsConfigured() {
		contextRunner
				.withPropertyValues(
						"communitybrain.ai.provider=openai",
						"communitybrain.ai.openai.api-key=test-key")
				.run(context -> {
					assertThat(context).hasNotFailed();
					assertThat(context).hasSingleBean(LLMClient.class);
					assertThat(context.getBean(LLMClient.class)).isInstanceOf(OpenAILLMClient.class);
					assertThat(context).doesNotHaveBean(FakeLLMClient.class);
				});
	}

	@Test
	void selectsOnlyFakeClientWhenTestsExplicitlyConfigureFakeProvider() {
		contextRunner
				.withPropertyValues("communitybrain.ai.provider=fake")
				.run(context -> {
					assertThat(context).hasNotFailed();
					assertThat(context).hasSingleBean(LLMClient.class);
					assertThat(context.getBean(LLMClient.class)).isInstanceOf(FakeLLMClient.class);
					assertThat(context).doesNotHaveBean(OpenAILLMClient.class);
				});
	}

	@Test
	void failsStartupWhenOpenAIProviderHasNoApiKey() {
		contextRunner
				.withPropertyValues(
						"communitybrain.ai.provider=openai",
						"communitybrain.ai.openai.api-key=")
				.run(context -> {
					assertThat(context).hasFailed();
					assertThat(context.getStartupFailure())
							.hasRootCauseMessage(
									"OPENAI_API_KEY must be configured when the OpenAI provider is enabled");
				});
	}
}
