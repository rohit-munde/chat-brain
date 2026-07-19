package com.chatbrain.proactive;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class ProactiveCommentaryMetrics {

	private final Counter generated;
	private final Counter published;
	private final Counter skipped;
	private final Timer latency;

	public ProactiveCommentaryMetrics(MeterRegistry registry) {
		generated = registry.counter("chatbrain.proactive.comments.generated");
		published = registry.counter("chatbrain.proactive.comments.published");
		skipped = registry.counter("chatbrain.proactive.comments.skipped");
		latency = registry.timer("chatbrain.proactive.latency");
	}

	public void recordGenerated() { generated.increment(); }
	public void recordPublished() { published.increment(); }
	public void recordSkipped() { skipped.increment(); }
	public void recordLatency(Duration duration) { latency.record(duration); }

	public Snapshot snapshot() {
		return new Snapshot(
				(long) generated.count(),
				(long) published.count(),
				(long) skipped.count(),
				latency.mean(java.util.concurrent.TimeUnit.MILLISECONDS));
	}

	public record Snapshot(long generated, long published, long skipped, double averageLatencyMillis) { }
}
