package com.chatbrain.comedy;

public enum ComedyTheme {
	DOCKER_SAGA("Docker Saga"),
	REDIS_SAGA("Redis Saga"),
	SPRING_RESTART_COUNTER("Spring Restart Counter"),
	COFFEE_COUNTER("Coffee Counter"),
	BROWSER_TAB_COUNTER("Browser Tab Counter"),
	TODO_COUNTER("TODO Counter"),
	BUILD_FAILURE_COUNTER("Build Failure Counter"),
	MAVEN_DOWNLOAD_SAGA("Maven Download Saga"),
	MERGE_CONFLICT_SAGA("Git Merge Conflict Saga");

	private final String displayName;

	ComedyTheme(String displayName) {
		this.displayName = displayName;
	}

	public String displayName() {
		return displayName;
	}
}
