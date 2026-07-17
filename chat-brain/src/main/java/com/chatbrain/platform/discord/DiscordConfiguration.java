package com.chatbrain.platform.discord;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.EnumSet;

@Configuration(proxyBeanMethods = false)
@ConfigurationProperties(prefix = "chatbrain.discord")
@ConditionalOnProperty(name = "chatbrain.discord.enabled", havingValue = "true")
public class DiscordConfiguration {

	private boolean enabled;
	private String botToken;
	private String guildId;
	private String channelId;

	JDA createClient(DiscordGatewayListener gatewayListener) {
		validateRequiredConfiguration();
		return JDABuilder.createLight(botToken, EnumSet.of(
				GatewayIntent.GUILD_MEMBERS,
				GatewayIntent.GUILD_MESSAGES,
				GatewayIntent.MESSAGE_CONTENT))
				.addEventListeners(gatewayListener)
				.build();
	}

	private void validateRequiredConfiguration() {
		requireConfigured(botToken, "chatbrain.discord.bot-token");
		requireConfigured(guildId, "chatbrain.discord.guild-id");
		requireConfigured(channelId, "chatbrain.discord.channel-id");
	}

	private void requireConfigured(String value, String propertyName) {
		if (value == null || value.isBlank()) {
			throw new IllegalStateException(propertyName + " must be configured when Discord is enabled");
		}
	}

	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	public String getBotToken() {
		return botToken;
	}

	public void setBotToken(String botToken) {
		this.botToken = botToken;
	}

	public String getGuildId() {
		return guildId;
	}

	public void setGuildId(String guildId) {
		this.guildId = guildId;
	}

	public String getChannelId() {
		return channelId;
	}

	public void setChannelId(String channelId) {
		this.channelId = channelId;
	}
}
