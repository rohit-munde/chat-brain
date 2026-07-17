package com.chatbrain.platform.discord;

import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "chatbrain.discord.enabled", havingValue = "true")
public class DiscordMessageSender {

	private static final Logger LOGGER = LoggerFactory.getLogger(DiscordMessageSender.class);

	private final DiscordPlatformAdapter adapter;
	private final DiscordConfiguration configuration;

	public DiscordMessageSender(
			DiscordPlatformAdapter adapter,
			DiscordConfiguration configuration) {
		this.adapter = adapter;
		this.configuration = configuration;
	}

	public void sendMessage(String message) {
		if (message == null || message.isBlank()) {
			throw new IllegalArgumentException("Discord message must not be blank");
		}

		adapter.currentClient().ifPresentOrElse(
				client -> sendToConfiguredGuild(
						client.getGuildById(configuration.getGuildId()),
						message),
				() -> LOGGER.warn("Cannot send Discord message because the client is not connected"));
	}

	private void sendToConfiguredGuild(Guild guild, String message) {
		if (guild == null) {
			LOGGER.error("Configured Discord guild {} was not found", configuration.getGuildId());
			return;
		}
		sendToConfiguredChannel(guild.getTextChannelById(configuration.getChannelId()), message);
	}

	private void sendToConfiguredChannel(TextChannel channel, String message) {
		if (channel == null) {
			LOGGER.error("Configured Discord channel {} was not found", configuration.getChannelId());
			return;
		}

		channel.sendMessage(message).queue(
				sentMessage -> LOGGER.info("Sent message to Discord channel {}", channel.getId()),
				failure -> LOGGER.error("Failed to send message to Discord channel {}",
						channel.getId(), failure));
	}
}
