package com.chatbrain.config;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.YouTubeScopes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.List;

@Configuration
@ConditionalOnProperty(name = "chatbrain.youtube.enabled", havingValue = "true")
public class YouTubeConfiguration {

	private static final Logger LOGGER = LoggerFactory.getLogger(YouTubeConfiguration.class);
	private static final String APPLICATION_NAME = "ChatBrain";
	private static final GsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

	@Bean
	YouTube youtubeClient(
			@Value("${chatbrain.youtube.credentials-path}") Path credentialsPath,
			@Value("${chatbrain.youtube.tokens-directory}") Path tokensDirectory,
			@Value("${chatbrain.youtube.oauth-port}") int oauthPort) {
		try {
			Credential credential = authorize(credentialsPath, tokensDirectory, oauthPort);
			return new YouTube.Builder(
					GoogleNetHttpTransport.newTrustedTransport(),
					JSON_FACTORY,
					credential)
					.setApplicationName(APPLICATION_NAME)
					.build();
		} catch (IOException | GeneralSecurityException exception) {
			LOGGER.error("YouTube OAuth or API client initialization failed: {}", exception.getMessage(), exception);
			throw new IllegalStateException("Unable to configure the YouTube API client", exception);
		} catch (IllegalStateException exception) {
			LOGGER.error("YouTube OAuth configuration failed: {}", exception.getMessage());
			throw exception;
		}
	}

	private Credential authorize(Path credentialsPath, Path tokensDirectory, int oauthPort)
			throws IOException, GeneralSecurityException {
		if (!Files.isRegularFile(credentialsPath)) {
			throw new IllegalStateException(
					"YouTube OAuth credentials file not found: " + credentialsPath.toAbsolutePath());
		}

		GoogleClientSecrets clientSecrets;
		try (Reader reader = Files.newBufferedReader(credentialsPath)) {
			clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, reader);
		}

		GoogleAuthorizationCodeFlow authorizationCodeFlow = new GoogleAuthorizationCodeFlow.Builder(
				GoogleNetHttpTransport.newTrustedTransport(),
				JSON_FACTORY,
				clientSecrets,
				List.of(YouTubeScopes.YOUTUBE_READONLY))
				.setDataStoreFactory(new FileDataStoreFactory(tokensDirectory.toFile()))
				.setAccessType("offline")
				.build();

		LocalServerReceiver receiver = new LocalServerReceiver.Builder()
				.setPort(oauthPort)
				.build();
		return new AuthorizationCodeInstalledApp(authorizationCodeFlow, receiver)
				.authorize("chatbrain");
	}
}
