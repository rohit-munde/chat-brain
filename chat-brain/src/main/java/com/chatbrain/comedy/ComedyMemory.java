package com.chatbrain.comedy;

import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class ComedyMemory {

	private final Map<ComedyTheme, Integer> themeCounts = new EnumMap<>(ComedyTheme.class);

	public synchronized List<ComedyThemeCount> observe(String text) {
		String value = text == null ? "" : text.toLowerCase(Locale.ROOT);
		incrementWhen(value, ComedyTheme.DOCKER_SAGA, "docker", "container");
		incrementWhen(value, ComedyTheme.REDIS_SAGA, "redis");
		incrementWhen(value, ComedyTheme.SPRING_RESTART_COUNTER, "spring restart", "restarting spring");
		incrementWhen(value, ComedyTheme.COFFEE_COUNTER, "coffee", "chai");
		incrementWhen(value, ComedyTheme.BROWSER_TAB_COUNTER, "browser tab", "tabs open");
		incrementWhen(value, ComedyTheme.TODO_COUNTER, "todo");
		incrementWhen(value, ComedyTheme.BUILD_FAILURE_COUNTER, "build failed", "compilation failed", "tests failed");
		incrementWhen(value, ComedyTheme.MAVEN_DOWNLOAD_SAGA, "maven download", "downloading dependencies");
		incrementWhen(value, ComedyTheme.MERGE_CONFLICT_SAGA, "merge conflict", "rebase conflict");
		return activeThemes();
	}

	public synchronized List<ComedyThemeCount> activeThemes() {
		return themeCounts.entrySet().stream()
				.map(entry -> new ComedyThemeCount(entry.getKey(), entry.getValue()))
				.sorted((left, right) -> Integer.compare(right.count(), left.count()))
				.toList();
	}

	private void incrementWhen(String text, ComedyTheme theme, String... terms) {
		for (String term : terms) {
			if (text.contains(term)) {
				themeCounts.merge(theme, 1, Integer::sum);
				return;
			}
		}
	}
}
