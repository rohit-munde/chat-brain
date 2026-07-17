package com.chatbrain.routing;

import com.chatbrain.events.UserResolvedEvent;
import com.chatbrain.platform.PlatformReplyDispatcher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class MessageRoutingListener {

	private final MessageRouter messageRouter;
	private final PlatformReplyDispatcher replyDispatcher;

	public MessageRoutingListener(
			MessageRouter messageRouter,
			PlatformReplyDispatcher replyDispatcher) {
		this.messageRouter = messageRouter;
		this.replyDispatcher = replyDispatcher;
	}

	@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
	public void onUserResolved(UserResolvedEvent event) {
		messageRouter.route(event).ifPresent(reply -> replyDispatcher.dispatch(
				event.getPlatformIdentity().getPlatform(),
				reply));
	}
}
