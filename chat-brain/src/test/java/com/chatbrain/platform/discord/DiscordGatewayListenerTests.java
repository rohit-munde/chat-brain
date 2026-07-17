package com.chatbrain.platform.discord;

import com.chatbrain.events.ChatMessageEvent;
import com.chatbrain.events.MemberJoinedEvent;
import com.chatbrain.platform.Platform;
import com.chatbrain.platform.events.PlatformMessageMapper;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.unions.MessageChannelUnion;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.ApplicationEventPublisher;

import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DiscordGatewayListenerTests {

	private DiscordGatewayListener listener;
	private ApplicationEventPublisher eventPublisher;

	@BeforeEach
	void setUp() {
		DiscordConfiguration configuration = new DiscordConfiguration();
		configuration.setGuildId("guild-123");
		configuration.setChannelId("channel-123");
		eventPublisher = mock(ApplicationEventPublisher.class);
		listener = new DiscordGatewayListener(
				configuration,
				new PlatformMessageMapper(),
				eventPublisher);
	}

	@Test
	void publishesMemberJoinedEventForConfiguredGuild() {
		GuildMemberJoinEvent gatewayEvent = mock(GuildMemberJoinEvent.class);
		Guild guild = mock(Guild.class);
		Member member = mock(Member.class);
		User user = mock(User.class);
		when(gatewayEvent.getGuild()).thenReturn(guild);
		when(guild.getId()).thenReturn("guild-123");
		when(gatewayEvent.getMember()).thenReturn(member);
		when(member.getUser()).thenReturn(user);
		when(member.getEffectiveName()).thenReturn("Rohit Munde");
		when(user.getId()).thenReturn("user-123");
		when(user.getName()).thenReturn("indieDeveloper");

		listener.onGuildMemberJoin(gatewayEvent);

		ArgumentCaptor<MemberJoinedEvent> eventCaptor = ArgumentCaptor.forClass(MemberJoinedEvent.class);
		verify(eventPublisher).publishEvent(eventCaptor.capture());
		MemberJoinedEvent publishedEvent = eventCaptor.getValue();
		assertThat(publishedEvent.getPlatform()).isEqualTo(Platform.DISCORD);
		assertThat(publishedEvent.getPlatformUserId()).isEqualTo("user-123");
		assertThat(publishedEvent.getUsername()).isEqualTo("indieDeveloper");
		assertThat(publishedEvent.getDisplayName()).isEqualTo("Rohit Munde");
		assertThat(publishedEvent.getTimestamp()).isNotNull();
	}

	@Test
	void mapsConfiguredChannelMessageToExistingChatMessageEvent() {
		MessageReceivedEvent gatewayEvent = mock(MessageReceivedEvent.class);
		Guild guild = mock(Guild.class);
		MessageChannelUnion channel = mock(MessageChannelUnion.class);
		Member member = mock(Member.class);
		User user = mock(User.class);
		Message message = mock(Message.class);
		OffsetDateTime messageTime = OffsetDateTime.parse("2026-07-17T13:52:39Z");
		when(gatewayEvent.isFromGuild()).thenReturn(true);
		when(gatewayEvent.getGuild()).thenReturn(guild);
		when(guild.getId()).thenReturn("guild-123");
		when(gatewayEvent.getChannel()).thenReturn(channel);
		when(channel.getId()).thenReturn("channel-123");
		when(gatewayEvent.getMember()).thenReturn(member);
		when(member.getEffectiveName()).thenReturn("Rohit Munde");
		when(gatewayEvent.getAuthor()).thenReturn(user);
		when(user.getId()).thenReturn("user-123");
		when(user.getName()).thenReturn("indieDeveloper");
		when(user.isBot()).thenReturn(false);
		when(gatewayEvent.getMessage()).thenReturn(message);
		when(message.getContentRaw()).thenReturn("Hello from Discord");
		when(message.getTimeCreated()).thenReturn(messageTime);

		listener.onMessageReceived(gatewayEvent);

		ArgumentCaptor<ChatMessageEvent> eventCaptor = ArgumentCaptor.forClass(ChatMessageEvent.class);
		verify(eventPublisher).publishEvent(eventCaptor.capture());
		ChatMessageEvent publishedEvent = eventCaptor.getValue();
		assertThat(publishedEvent.getPlatform()).isEqualTo("DISCORD");
		assertThat(publishedEvent.getPlatformUserId()).isEqualTo("user-123");
		assertThat(publishedEvent.getHandle()).isEqualTo("indieDeveloper");
		assertThat(publishedEvent.getDisplayName()).isEqualTo("Rohit Munde");
		assertThat(publishedEvent.getMessage()).isEqualTo("Hello from Discord");
	}
}
