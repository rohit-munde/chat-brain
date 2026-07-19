package com.chatbrain.comedy;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ComedyStyleSelector {

	private final List<ComedyStyleStrategy> strategies;

	public ComedyStyleSelector(List<ComedyStyleStrategy> strategies) {
		this.strategies = List.copyOf(strategies);
	}

	public ComedyStyle select(ComedySituation situation) {
		return strategies.stream()
				.map(strategy -> strategy.select(situation))
				.flatMap(java.util.Optional::stream)
				.findFirst()
				.orElse(ComedyStyle.NO_HUMOR);
	}
}
