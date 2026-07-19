# ChatBrain Architecture

## AI Decision Engine

ChatBrain uses Spring application events to keep platform reception independent from identity,
memory, and AI concerns. Incoming YouTube and Discord messages are normalized into a
`PlatformMessage`, mapped to a `ChatMessageEvent`, and published to the application event bus.

The current AI path is:

```text
ChatMessageEvent
        ↓
IdentityResolver
        ↓
MemoryRetriever
        ↓
PromptBuilder
        ↓
LLMClient
        ↓
AIResponseDecisionParser
        ↓
AIResponseDecision
        ↓
AIOrchestrator executes REPLY or IGNORE
        ↓
YouTubePublisher (REPLY only)
        ↓
MemoryLearningService
```

`IdentityResolver` is an ordered listener and runs before AI orchestration. The
`AIOrchestrationListener` then delegates to `AIOrchestrator`, which continues to coordinate the
existing memory retrieval, prompt construction, LLM invocation, publishing, and memory-learning
services.

## Decision model

The AI Decision Engine is the combination of the decision policy owned by `PromptBuilder`, the
structured output returned through `LLMClient`, the validation performed by
`AIResponseDecisionParser`, and the exhaustive action execution in `AIOrchestrator`. It is the
single behavioral path that decides whether ChatBrain participates in a conversation.

The LLM is instructed to return one of two JSON decisions:

```json
{
  "action": "REPLY",
  "reply": "Hello Rohit!"
}
```

or:

```json
{
  "action": "IGNORE"
}
```

`AIResponseDecision` is the strongly typed application model. `AIResponseAction` currently
supports:

- `REPLY`: publish the nonblank `reply` through `YouTubePublisher`.
- `IGNORE`: publish nothing.

The enum provides a deliberate extension point for future actions without placing those actions
in the orchestrator before they are required.

`AIResponseDecision` contains only the V1 execution contract: `action` and optional `reply`.
`AIResponseDecisionParser` owns JSON deserialization and validation. It accepts plain JSON and
JSON enclosed in a Markdown code fence. A `REPLY` decision must contain nonblank reply text, while
an `IGNORE` decision does not require a reply. Unknown JSON properties are ignored for backwards
compatibility. If JSON parsing or validation fails, the parser creates a safe `REPLY` decision
whose reply is the complete original LLM output. A blank provider response uses a nonblank safe
fallback so malformed output cannot crash publication or the event pipeline.

## Prompt contract

`PromptBuilder` still owns all prompt formatting. In addition to platform identity, recent
memories, timestamp, and the current message, it defines ChatBrain as an intelligent invisible
co-host rather than a mention-driven chatbot. The decision policy favors quality over quantity:
technical questions, debugging, architecture, useful context, misconceptions, interesting
opinions, and entertaining opportunities normally merit `REPLY`; emoji-only messages, short
acknowledgements, spam, and low-value interruptions normally merit `IGNORE`.

`LLMClient` remains unchanged:

```java
public interface LLMClient {
    String generateReply(String prompt);
}
```

Keeping the provider boundary as raw text means `FakeLLMClient` and `OpenAILLMClient` remain
isolated from application decisions. Parsing the provider output is an application concern and is
therefore performed immediately after the LLM call.

## Decision execution and memory learning

`AIOrchestrator` executes decisions with an exhaustive enum switch:

- `REPLY` calls `YouTubePublisher.publish(reply)`.
- `IGNORE` logs the decision and does not call the publisher.

Memory learning is invoked after either action, preserving the existing learning lifecycle.
Decision execution does not change identity resolution, memory retrieval, or memory persistence.

## Design rationale

This implementation fits the current architecture because:

- identity resolution, memory retrieval, prompt building, LLM providers, publishing, and memory
  persistence remain unchanged in responsibility;
- `LLMClient` remains the provider abstraction and does not execute application behavior;
- parsing is isolated in one focused component instead of being mixed into `PromptBuilder`, the
  OpenAI client, or the orchestrator;
- `AIOrchestrator` continues to coordinate the workflow and is the correct existing location for
  executing the resulting action;
- malformed model output degrades to the previous plain-text reply behavior;
- future actions can extend `AIResponseAction` and the orchestrator's exhaustive decision switch
  without changing platform adapters or LLM implementations.

Potential future actions include `MODERATE`, `REMEMBER_ONLY`, `PRODUCER_NOTIFICATION`,
`TRIGGER_CTA`, and `TRIGGER_COMEDY`. They are intentionally not present in the enum today; adding
one will require an explicit execution branch, keeping unsupported behavior impossible by
default.

No moderation, command handling, or additional decision actions are implemented in this
milestone.

## YouTube API observability

YouTube Data API observability is isolated under `com.chatbrain.platform.youtube.metrics`.
`YouTubeApiMetricsService` owns all Micrometer instruments, request timing, application counters,
and runtime snapshot formatting. The Google API client is not referenced by the metrics module;
callers provide a small checked operation to the generic `recordApiCall` method.

The four currently used endpoints are represented by `YouTubeApiEndpoint` and measured
independently:

- `liveBroadcasts.list`
- `liveChatMessages.list`
- `liveChatMessages.insert`
- `channels.list`

For every endpoint, the module records request, success, and failure counters plus a timer from
which total and average latency are calculated. A request is successful when the API operation
returns without an `IOException` or runtime failure. Failed requests remain included in latency
measurement.

The module also records application-level YouTube observations:

- chat messages accepted into the application event pipeline;
- YouTube messages assigned an `IGNORE` AI decision;
- replies successfully inserted into YouTube Live Chat;
- active broadcasts successfully discovered;
- author identity cache hits and misses;
- unique author cache entries enriched during the process;
- the latest effective polling interval after applying YouTube's recommendation and ChatBrain's
  configured minimum.

AI decision observation uses the platform-neutral `AIResponseDecisionObserver` interface.
`AIOrchestrator` notifies observers after executing a decision, while the YouTube metrics
implementation counts only ignored events whose originating platform is YouTube. Observer
failures are contained so observability cannot interrupt replies or memory learning.

### Micrometer meters

Endpoint meters use an `endpoint` tag:

```text
chatbrain.youtube.api.requests
chatbrain.youtube.api.successes
chatbrain.youtube.api.failures
chatbrain.youtube.api.latency
```

Application meters are:

```text
chatbrain.youtube.polling.interval.milliseconds
chatbrain.youtube.messages.received
chatbrain.youtube.messages.ignored
chatbrain.youtube.replies.published
chatbrain.youtube.broadcasts.discovered
chatbrain.youtube.authors.enriched
chatbrain.youtube.author.cache.hits
chatbrain.youtube.author.cache.misses
```

`YouTubeApiMetricsService.snapshot()` returns an immutable `YouTubeApiMetrics` value containing
all endpoint and application measurements. `formatSnapshot()` provides a human-readable runtime
report suitable for logging or operational diagnostics. No controller, UI, scheduled logger, or
additional Actuator exposure is introduced in this milestone.

This design leaves request behavior unchanged and prepares the metrics for future Prometheus,
Grafana, Actuator, estimated-quota, and dashboard integrations. Adding another endpoint requires
an enum entry and wrapping its API operation; it does not require duplicating counter or timer
logic.
