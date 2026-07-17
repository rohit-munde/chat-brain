package com.chatbrain.platform.discord;

import com.chatbrain.platform.Platform;
import com.chatbrain.platform.PlatformAdapter;
import jakarta.annotation.PreDestroy;
import net.dv8tion.jda.api.JDA;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
@ConditionalOnProperty(name = "chatbrain.discord.enabled", havingValue = "true")
public final class DiscordPlatformAdapter implements PlatformAdapter {

	private static final Logger LOGGER = LoggerFactory.getLogger(DiscordPlatformAdapter.class);
	private static final Duration SHUTDOWN_TIMEOUT = Duration.ofSeconds(10);

	private final DiscordConfiguration configuration;
	private final DiscordGatewayListener gatewayListener;
	private volatile JDA client;

	public DiscordPlatformAdapter(
			DiscordConfiguration configuration,
			DiscordGatewayListener gatewayListener) {
		this.configuration = configuration;
		this.gatewayListener = gatewayListener;
	}

	@Override
	public Platform getPlatform() {
		return Platform.DISCORD;
	}

	@Override
	@EventListener(ApplicationReadyEvent.class)
	public synchronized void start() {
		if (client != null) {
			return;
		}

		JDA connectedClient = null;
		try {
			connectedClient = configuration.createClient(gatewayListener);
			connectedClient.awaitReady();
			client = connectedClient;
			LOGGER.info("Connected to Discord successfully as {}", connectedClient.getSelfUser().getName());
		} catch (InterruptedException exception) {
			shutdownAfterFailedConnection(connectedClient);
			Thread.currentThread().interrupt();
			throw new IllegalStateException("Interrupted while connecting to Discord", exception);
		} catch (RuntimeException exception) {
			shutdownAfterFailedConnection(connectedClient);
			throw new IllegalStateException("Failed to connect to Discord", exception);
		}
	}

	private void shutdownAfterFailedConnection(JDA connectedClient) {
		if (connectedClient != null) {
			connectedClient.shutdownNow();
		}
	}

	@Override
	@PreDestroy
	public synchronized void stop() {
		JDA activeClient = client;
		client = null;
		if (activeClient == null) {
			return;
		}

		LOGGER.info("Disconnecting from Discord...");
		activeClient.shutdown();
		try {
			if (!activeClient.awaitShutdown(SHUTDOWN_TIMEOUT)) {
				LOGGER.warn("Discord did not disconnect within {}; forcing shutdown", SHUTDOWN_TIMEOUT);
				activeClient.shutdownNow();
			}
		} catch (InterruptedException exception) {
			Thread.currentThread().interrupt();
			activeClient.shutdownNow();
			LOGGER.warn("Interrupted while disconnecting from Discord; forced shutdown");
		}
	}

	Optional<JDA> currentClient() {
		return Optional.ofNullable(client);
	}
}
