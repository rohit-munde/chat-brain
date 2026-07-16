package com.chatbrain.listeners;

import com.chatbrain.events.UserResolvedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class UserResolvedListener {

	private static final Logger LOGGER = LoggerFactory.getLogger(UserResolvedListener.class);

	@EventListener
	public void onUserResolved(UserResolvedEvent event) {
		LOGGER.info("""
				----------------------------------
				Platform : {}
				Channel ID : {}
				Visible Name : {}
				Message : {}
				Timestamp : {}
				----------------------------------""",
				event.getPlatformIdentity().getPlatform(),
				event.getPlatformIdentity().getChannelId(),
				event.getPlatformIdentity().getVisibleName(),
				event.getOriginalEvent().getMessage(),
				event.getOriginalEvent().getTimestamp());
	}
}
