package com.chatbrain.comedy;

import com.chatbrain.events.EventType;
import org.springframework.stereotype.Component;

import java.util.Locale;

@Component
public class ComedyOpportunityDetector {

	public ComedyOpportunity detect(String text, EventType eventType) {
		String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT).trim();
		ComedyOpportunity eventOpportunity = fromEvent(eventType);
		if (eventOpportunity != ComedyOpportunity.NONE) {
			return eventOpportunity;
		}
		if (normalized.isBlank() || isSimpleGreeting(normalized)) {
			return ComedyOpportunity.NONE;
		}
		if (contains(normalized, "data loss", "security incident", "production outage",
				"credential leak", "credentials leaked", "breach", "ransomware")) {
			return ComedyOpportunity.SERIOUS_TECHNICAL;
		}
		if (contains(normalized, "docker", "container")) return ComedyOpportunity.DOCKER_FAILURE;
		if (contains(normalized, "compile", "compiler", "compilation", "cannot find symbol")) return ComedyOpportunity.COMPILATION_ERROR;
		if (contains(normalized, "debugging", "debugged", "still debugging")) return ComedyOpportunity.LONG_DEBUGGING;
		if (contains(normalized, "dependency", "maven", "gradle", "jar hell")) return ComedyOpportunity.DEPENDENCY_HELL;
		if (contains(normalized, "spring restart", "restarting spring", "devtools restart")) return ComedyOpportunity.SPRING_RESTART;
		if (contains(normalized, "build failed", "tests failed", "test failed")) return ComedyOpportunity.BUILD_FAILURE;
		if (contains(normalized, "configuration", "config error", "yaml", "properties")) return ComedyOpportunity.CONFIGURATION_ERROR;
		if (contains(normalized, "overengineer", "abstraction for", "factory factory")) return ComedyOpportunity.OVERENGINEERING;
		if (contains(normalized, "feature creep", "one more feature", "scope creep")) return ComedyOpportunity.FEATURE_CREEP;
		if (contains(normalized, "merge conflict", "rebase conflict")) return ComedyOpportunity.MERGE_CONFLICT;
		if (contains(normalized, "coffee", "chai")) return ComedyOpportunity.COFFEE;
		if (contains(normalized, "engineering college", "college viva", "campus placement")) return ComedyOpportunity.INDIAN_ENGINEERING_CONTEXT;
		if (contains(normalized, "indian office", "timesheet", "manager ping", "notice period")) return ComedyOpportunity.INDIAN_OFFICE_CONTEXT;
		if (contains(normalized, "startup", "runway", "funding", "pivot")) return ComedyOpportunity.STARTUP_CONTEXT;
		if (contains(normalized, "bollywood", "movie dialogue", "filmy")) return ComedyOpportunity.BOLLYWOOD_CONTEXT;
		if (contains(normalized, "cricket", "wicket", "innings", "third umpire")) return ComedyOpportunity.CRICKET_CONTEXT;
		if (contains(normalized, "rohit", "roast him", "skill issue")) return ComedyOpportunity.FRIENDLY_ROAST;
		if (normalized.endsWith("?") || startsQuestion(normalized)) return ComedyOpportunity.TECHNICAL_QUESTION;
		return ComedyOpportunity.NONE;
	}

	private ComedyOpportunity fromEvent(EventType eventType) {
		if (eventType == null) return ComedyOpportunity.NONE;
		return switch (eventType) {
			case BUILD_SUCCEEDED, APPLICATION_STARTED -> ComedyOpportunity.SUCCESS;
			case COMPILATION_FAILED -> ComedyOpportunity.COMPILATION_ERROR;
			case LONG_DEBUG_SESSION -> ComedyOpportunity.LONG_DEBUGGING;
			case FEATURE_COMPLETED, MILESTONE_REACHED -> ComedyOpportunity.MILESTONE;
			default -> ComedyOpportunity.NONE;
		};
	}

	private boolean isSimpleGreeting(String value) {
		return value.matches("(hi+|hello+|hey+|namaste|good (morning|evening)|yo)[!. ]*");
	}

	private boolean startsQuestion(String value) {
		return value.startsWith("why ") || value.startsWith("how ")
				|| value.startsWith("what ") || value.startsWith("when ")
				|| value.startsWith("should ") || value.startsWith("can ");
	}

	private boolean contains(String value, String... terms) {
		for (String term : terms) {
			if (value.contains(term)) return true;
		}
		return false;
	}
}
