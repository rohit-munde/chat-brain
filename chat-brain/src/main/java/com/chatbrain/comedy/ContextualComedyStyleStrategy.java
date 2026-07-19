package com.chatbrain.comedy;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;

@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class ContextualComedyStyleStrategy implements ComedyStyleStrategy {

	@Override
	public Optional<ComedyStyle> select(ComedySituation situation) {
		if (seriousMood(situation.streamMood())
				|| situation.opportunity() == ComedyOpportunity.NONE
				|| situation.opportunity() == ComedyOpportunity.SERIOUS_TECHNICAL) {
			return Optional.of(ComedyStyle.NO_HUMOR);
		}
		if (situation.callbackAvailable()) {
			return Optional.of(ComedyStyle.CALLBACK_HUMOR);
		}
		return Optional.of(switch (situation.opportunity()) {
			case TECHNICAL_QUESTION -> ComedyStyle.TEACHING_THROUGH_HUMOR;
			case FRIENDLY_ROAST, VIEWER_TROLLING -> ComedyStyle.FRIENDLY_ROAST;
			case MILESTONE, SUCCESS -> ComedyStyle.CELEBRATION_HUMOR;
			case INDIAN_ENGINEERING_CONTEXT -> ComedyStyle.INDIAN_ENGINEERING_CULTURE;
			case INDIAN_OFFICE_CONTEXT -> ComedyStyle.INDIAN_OFFICE_CULTURE;
			case STARTUP_CONTEXT -> ComedyStyle.STARTUP_CULTURE;
			case BOLLYWOOD_CONTEXT -> ComedyStyle.BOLLYWOOD_ANALOGY;
			case CRICKET_CONTEXT -> ComedyStyle.CRICKET_ANALOGY;
			case OVERENGINEERING, FEATURE_CREEP -> ComedyStyle.OBSERVATIONAL_HUMOR;
			case COFFEE, LATE_NIGHT_CODING -> ComedyStyle.DRY_SARCASM;
			default -> ComedyStyle.DEVELOPER_HUMOR;
		});
	}

	private boolean seriousMood(String mood) {
		String value = mood == null ? "" : mood.toLowerCase(Locale.ROOT);
		return value.contains("serious") || value.contains("incident")
				|| value.contains("security") || value.contains("sensitive");
	}
}
