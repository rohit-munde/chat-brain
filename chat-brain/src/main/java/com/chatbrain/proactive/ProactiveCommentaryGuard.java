package com.chatbrain.proactive;

import com.chatbrain.events.ChatMessageEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Locale;

@Component
public class ProactiveCommentaryGuard {

	private static final int COMMENT_HISTORY_SIZE = 10;
	private final ProactiveCommentaryProperties properties;
	private final ArrayDeque<String> recentComments = new ArrayDeque<>();
	private Instant lastPublishedAt;
	private boolean chatObservedSinceLastComment = true;

	public ProactiveCommentaryGuard(ProactiveCommentaryProperties properties) {
		this.properties = properties;
	}

	@EventListener
	public synchronized void onChatMessage(ChatMessageEvent ignored) {
		chatObservedSinceLastComment = true;
	}

	public synchronized boolean canAttempt(Instant now) {
		if (lastPublishedAt == null) {
			return true;
		}
		return chatObservedSinceLastComment
				&& !now.isBefore(lastPublishedAt.plus(properties.getMinimumCooldown()));
	}

	public synchronized boolean isSimilarToRecent(String comment) {
		String candidate = normalize(comment);
		return recentComments.stream().anyMatch(previous -> similar(previous, candidate));
	}

	public synchronized void recordPublished(String comment, Instant publishedAt) {
		lastPublishedAt = publishedAt;
		chatObservedSinceLastComment = false;
		recentComments.addLast(normalize(comment));
		while (recentComments.size() > COMMENT_HISTORY_SIZE) {
			recentComments.removeFirst();
		}
	}

	private boolean similar(String previous, String candidate) {
		return previous.equals(candidate)
				|| (candidate.length() >= 20
				&& (previous.contains(candidate) || candidate.contains(previous)));
	}

	private String normalize(String value) {
		return value.toLowerCase(Locale.ROOT)
				.replaceAll("[^a-z0-9 ]", " ")
				.replaceAll("\\s+", " ")
				.trim();
	}
}
