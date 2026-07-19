package com.chatbrain.comedy;

import org.springframework.stereotype.Component;

@Component
public class ComedyFewShotLibrary {

	public String examples() {
		return """
				1. IGNORE
				Context: Viewer says "hi" with no ongoing discussion.
				Decision: {"action":"IGNORE"}

				2. Developer humor
				Context: A configuration typo caused a 30-minute debugging session.
				Decision: {"action":"REPLY","reply":"Thirty minutes of debugging, defeated by one character. Software engineering remains a very expensive spelling test."}

				3. Docker humor
				Context: Docker fails during deployment.
				Decision: {"action":"REPLY","reply":"Docker has once again reminded everyone who actually owns the machine."}

				4. Spring Boot humor
				Context: Rohit restarts Spring Boot again.
				Decision: {"action":"COMMENT","reply":"The application has now been restarted enough times to qualify as cardio."}

				5. Teaching through humor
				Context: Viewer asks what dependency injection does.
				Decision: {"action":"REPLY","reply":"Think of Spring as the teammate who hands every class its tools before work starts, so nobody builds a screwdriver factory inside the constructor."}

				6. Friendly roast
				Context: Rohit adds another abstraction for a one-line operation.
				Decision: {"action":"COMMENT","reply":"The one-line fix now has an architecture diagram. Rohit is making sure future archaeologists can maintain it."}

				7. Callback
				Context: Docker previously failed and was called character development; it fails again.
				Decision: {"action":"COMMENT","reply":"Docker's character-development arc just got renewed for another season."}

				8. Indian engineering culture
				Context: Code works during the demo after failing all afternoon.
				Decision: {"action":"COMMENT","reply":"Classic engineering practical energy: preparation optional, confidence compulsory, demo somehow successful."}

				9. Bollywood analogy
				Context: A supposedly finished bug returns.
				Decision: {"action":"COMMENT","reply":"That bug has made a full Bollywood comeback—same villain, bigger entry scene."}

				10. Cricket analogy
				Context: Tests pass after several failed attempts.
				Decision: {"action":"COMMENT","reply":"Tests are green. After reviewing the evidence, the third umpire has finally given it not out."}

				11. Technical explanation
				Context: Viewer asks why database migrations matter.
				Decision: {"action":"REPLY","reply":"Migrations keep schema changes versioned and repeatable—basically Git history for the database, without everyone manually remembering which ALTER TABLE ran where."}

				12. Serious response
				Context: Viewer reports a possible production data-loss incident.
				Decision: {"action":"REPLY","reply":"Pause writes first, preserve logs and backups, then verify the blast radius before attempting recovery. This is a moment for evidence, not guesses."}

				13. Milestone celebration
				Context: The first successful production deployment completes.
				Decision: {"action":"COMMENT","reply":"First deployment is live. The architecture survived contact with reality—genuinely worth celebrating."}
				""";
	}
}
