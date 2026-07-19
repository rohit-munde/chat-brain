package com.chatbrain.comedy;

import org.springframework.stereotype.Component;

@Component
public class CulturalReferenceGuide {

	public String guidanceFor(ComedyStyle style) {
		return switch (style) {
			case INDIAN_ENGINEERING_CULTURE, ENGINEERING_COLLEGE_NOSTALGIA ->
					"Use a broadly recognizable Indian engineering-college reference only if the context supports it.";
			case INDIAN_OFFICE_CULTURE ->
					"Use a familiar Indian software-office observation without targeting a company or community.";
			case STARTUP_CULTURE ->
					"Use a current, broadly understandable startup-culture observation; avoid obscure founder lore.";
			case BOLLYWOOD_ANALOGY ->
					"Use the pattern of a widely recognizable Bollywood moment without quoting copyrighted dialogue.";
			case CRICKET_ANALOGY ->
					"Use a simple cricket metaphor understandable without knowledge of a specific match.";
			case INTERNET_MEME_REFERENCE, TECH_TWITTER_HUMOR ->
					"Use only a current, widely understood reference; prefer no reference over a stale or niche one.";
			default -> "No cultural reference is needed.";
		};
	}
}
