package com.chatbrain.proactive;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Objects;

@Component
@ConfigurationProperties(prefix = "communitybrain.ai.proactive")
public class ProactiveCommentaryProperties {

	private boolean enabled;
	private Duration minimumCooldown = Duration.ofMinutes(5);
	private String streamTitle = "Not provided";
	private String currentProject = "ChatBrain";
	private String currentCodingTopic = "Not provided";
	private String currentCodingPhase = "Not provided";
	private String streamMood = "Focused";

	public boolean isEnabled() { return enabled; }
	public void setEnabled(boolean enabled) { this.enabled = enabled; }
	public Duration getMinimumCooldown() { return minimumCooldown; }
	public void setMinimumCooldown(Duration minimumCooldown) {
		Duration value = Objects.requireNonNull(minimumCooldown, "minimumCooldown must not be null");
		if (value.isNegative()) {
			throw new IllegalArgumentException("minimumCooldown must not be negative");
		}
		this.minimumCooldown = value;
	}
	public String getStreamTitle() { return streamTitle; }
	public void setStreamTitle(String streamTitle) { this.streamTitle = streamTitle; }
	public String getCurrentProject() { return currentProject; }
	public void setCurrentProject(String currentProject) { this.currentProject = currentProject; }
	public String getCurrentCodingTopic() { return currentCodingTopic; }
	public void setCurrentCodingTopic(String currentCodingTopic) { this.currentCodingTopic = currentCodingTopic; }
	public String getCurrentCodingPhase() { return currentCodingPhase; }
	public void setCurrentCodingPhase(String currentCodingPhase) { this.currentCodingPhase = currentCodingPhase; }
	public String getStreamMood() { return streamMood; }
	public void setStreamMood(String streamMood) { this.streamMood = streamMood; }
}
