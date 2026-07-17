package com.chatbrain.platform.discord;

import com.chatbrain.events.ChatMessageEvent;
import com.chatbrain.events.MemberJoinedEvent;
import com.chatbrain.platform.Platform;
import com.chatbrain.platform.PlatformMessage;
import com.chatbrain.platform.events.PlatformMessageMapper;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

import java.time.Instant;

@Component
@ConditionalOnProperty(name = "chatbrain.discord.enabled", havingValue = "true")
public class DiscordGatewayListener extends ListenerAdapter {

	private static final Logger LOGGER = LoggerFactory.getLogger(DiscordGatewayListener.class);

	private final DiscordConfiguration configuration;
	private final PlatformMessageMapper messageMapper;
	private final ApplicationEventPublisher eventPublisher;

	public DiscordGatewayListener(
			DiscordConfiguration configuration,
			PlatformMessageMapper messageMapper,
			ApplicationEventPublisher eventPublisher) {
		this.configuration = configuration;
		this.messageMapper = messageMapper;
		this.eventPublisher = eventPublisher;
	}

	@Override
	public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {
		if (!configuration.getGuildId().equals(event.getGuild().getId())) {
			return;
		}

		Member member = event.getMember();
		User user = member.getUser();
		MemberJoinedEvent memberJoinedEvent = new MemberJoinedEvent(
				Platform.DISCORD,
				user.getId(),
				user.getName(),
				member.getEffectiveName());

		LOGGER.info("""
				----------------------------------
				Platform         : {}
				Platform User ID : {}
				Username         : {}
				Display Name     : {}
				----------------------------------""",
				memberJoinedEvent.getPlatform(),
				memberJoinedEvent.getPlatformUserId(),
				memberJoinedEvent.getUsername(),
				memberJoinedEvent.getDisplayName());
		eventPublisher.publishEvent(memberJoinedEvent);
	}

	@Override
	public void onMessageReceived(@NotNull MessageReceivedEvent event) {
		if (!event.isFromGuild()
				|| !configuration.getGuildId().equals(event.getGuild().getId())
				|| !configuration.getChannelId().equals(event.getChannel().getId())
				|| event.getAuthor().isBot()) {
			return;
		}

		Member member = event.getMember();
		if (member == null) {
			return;
		}

		User author = event.getAuthor();
		Instant timestamp = event.getMessage().getTimeCreated().toInstant();
		PlatformMessage platformMessage = new PlatformMessage(
				Platform.DISCORD,
				author.getId(),
				author.getName(),
				member.getEffectiveName(),
				event.getMessage().getContentRaw(),
				timestamp);

		LOGGER.info("""
				----------------------------------
				Platform         : {}
				Platform User ID : {}
				Username         : {}
				Display Name     : {}
				Message          : {}
				Timestamp        : {}
				----------------------------------""",
				platformMessage.platform(),
				platformMessage.platformUserId(),
				platformMessage.handle(),
				platformMessage.displayName(),
				platformMessage.message(),
				platformMessage.timestamp());

		ChatMessageEvent chatMessageEvent = messageMapper.toChatMessageEvent(platformMessage);
		eventPublisher.publishEvent(chatMessageEvent);
	}
}
