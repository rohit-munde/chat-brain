package com.chatbrain.ai;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class OpenAILLMClientTests {

	private MockRestServiceServer server;
	private OpenAILLMClient client;

	@BeforeEach
	void setUp() {
		RestClient.Builder restClientBuilder = RestClient.builder()
				.baseUrl("https://api.openai.com/v1");
		server = MockRestServiceServer.bindTo(restClientBuilder).build();
		client = new OpenAILLMClient(restClientBuilder.build(), "gpt-5.5");
	}

	@Test
	void sendsFinalPromptAndReturnsOnlyOutputText() {
		server.expect(once(), requestTo("https://api.openai.com/v1/responses"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(content().json("""
						{
						  "model": "gpt-5.5",
						  "input": "final prompt"
						}
						"""))
				.andRespond(withSuccess("""
						{
						  "output": [
						    {
						      "type": "message",
						      "content": [
						        {"type": "output_text", "text": "Generated reply"}
						      ]
						    }
						  ]
						}
						""", MediaType.APPLICATION_JSON));

		assertThat(client.generateReply("final prompt")).isEqualTo("Generated reply");
		server.verify();
	}

	@Test
	void returnsNonEmptyFallbackWhenOpenAIFails() {
		server.expect(once(), requestTo("https://api.openai.com/v1/responses"))
				.andRespond(withServerError());

		assertThat(client.generateReply("final prompt")).isNotBlank();
		server.verify();
	}
}
