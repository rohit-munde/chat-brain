package com.chatbrain.proactive;

import com.chatbrain.ai.AIResponseAction;
import com.chatbrain.ai.AIResponseDecision;
import com.chatbrain.ai.AIResponseDecisionParser;
import com.chatbrain.ai.LLMClient;
import com.chatbrain.ai.PromptBuilder;
import com.chatbrain.comedy.ComedyContext;
import com.chatbrain.comedy.ComedyIntelligence;
import com.chatbrain.events.ProactiveStreamEvent;
import com.chatbrain.platform.youtube.YouTubePublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class ProactiveCommentaryOrchestrator {

	private static final Logger LOGGER = LoggerFactory.getLogger(ProactiveCommentaryOrchestrator.class);
	private static final int TIMELINE_CONTEXT_SIZE = 10;

	private final ProactiveCommentaryProperties properties;
	private final StreamTimeline timeline;
	private final StreamContext streamContext;
	private final ProactiveCommentaryGuard guard;
	private final ComedyIntelligence comedyIntelligence;
	private final PromptBuilder promptBuilder;
	private final LLMClient llmClient;
	private final AIResponseDecisionParser decisionParser;
	private final YouTubePublisher youtubePublisher;
	private final ProactiveCommentaryMetrics metrics;

	public ProactiveCommentaryOrchestrator(
			ProactiveCommentaryProperties properties,
			StreamTimeline timeline,
			StreamContext streamContext,
			ProactiveCommentaryGuard guard,
			ComedyIntelligence comedyIntelligence,
			PromptBuilder promptBuilder,
			LLMClient llmClient,
			AIResponseDecisionParser decisionParser,
			YouTubePublisher youtubePublisher,
			ProactiveCommentaryMetrics metrics) {
		this.properties = properties;
		this.timeline = timeline;
		this.streamContext = streamContext;
		this.guard = guard;
		this.comedyIntelligence = comedyIntelligence;
		this.promptBuilder = promptBuilder;
		this.llmClient = llmClient;
		this.decisionParser = decisionParser;
		this.youtubePublisher = youtubePublisher;
		this.metrics = metrics;
	}

	public void process(ProactiveStreamEvent event) {
		timeline.append(event);
		if (!properties.isEnabled() || !guard.canAttempt(Instant.now())) {
			metrics.recordSkipped();
			LOGGER.info("Proactive commentary skipped by feature flag or frequency guard [event={}]",
					event.getEventType());
			return;
		}

		Instant startedAt = Instant.now();
		try {
			ProactiveCommentaryContext context = streamContext.snapshot(
					event, timeline.recentEntries(TIMELINE_CONTEXT_SIZE));
			ComedyContext comedyContext = comedyIntelligence.analyze(event);
			String prompt = promptBuilder.buildProactive(context, comedyContext);
			AIResponseDecision decision = decisionParser.parse(llmClient.generateReply(prompt));
			execute(decision);
		} catch (RuntimeException exception) {
			metrics.recordSkipped();
			LOGGER.error("Proactive commentary failed [event={}]: {}",
					event.getEventType(), exception.getMessage(), exception);
		} finally {
			metrics.recordLatency(Duration.between(startedAt, Instant.now()));
		}
	}

	private void execute(AIResponseDecision decision) {
		if (decision.action() != AIResponseAction.COMMENT) {
			metrics.recordSkipped();
			LOGGER.info("Proactive commentary chose silence [action={}]", decision.action());
			return;
		}

		metrics.recordGenerated();
		if (guard.isSimilarToRecent(decision.reply())) {
			metrics.recordSkipped();
			LOGGER.info("Proactive commentary skipped because it is similar to a recent comment");
			return;
		}

		youtubePublisher.publish(decision.reply());
		Instant publishedAt = Instant.now();
		guard.recordPublished(decision.reply(), publishedAt);
		streamContext.recordAiComment(decision.reply());
		metrics.recordPublished();
		LOGGER.info("Proactive commentary published");
	}
}
