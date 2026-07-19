package com.chatbrain.comedy;

import java.util.Optional;

public interface ComedyStyleStrategy {

	Optional<ComedyStyle> select(ComedySituation situation);
}
