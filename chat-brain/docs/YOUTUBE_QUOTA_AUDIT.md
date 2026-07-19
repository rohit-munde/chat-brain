# YouTube Data API Engineering Audit

No code was modified. This report describes the current production implementation as of July 19, 2026.

## Executive summary

ChatBrain currently uses four YouTube Data API methods:

1. `liveBroadcasts.list` — discover the active broadcast and live-chat ID.
2. `liveChatMessages.list` — poll incoming chat messages.
3. `channels.list` — enrich each unique author with a handle and channel title.
4. `liveChatMessages.insert` — publish ChatBrain responses.

The main quota risks are:

- REST polling continues for the entire livestream.
- Every unique author triggers a channel lookup once per application process.
- Every `REPLY` decision triggers a YouTube insert—even if the input came from Discord.
- When no active broadcast exists, discovery runs every 30 seconds.
- Discord-originated replies can trigger additional broadcast discovery when no session is cached.
- The implementation uses `liveChatMessages.list`, while current YouTube documentation explicitly recommends `streamList` as the most efficient way to consume chat.

For an active four-hour stream, the polling loop can make up to 1,440 `liveChatMessages.list` requests. The exact current quota cost of Live Streaming methods is not explicitly published in the current quota table, so this report provides both an official minimum and a conservative legacy-cost scenario.

---

# 1. Every YouTube API endpoint used

## 1. `liveBroadcasts.list`

HTTP request:

```http
GET https://www.googleapis.com/youtube/v3/liveBroadcasts
```

Parameters:

```text
part=snippet
broadcastStatus=active
```

Called from:

- `LiveChatSessionManager.discoverSession()`

Class:

- `LiveChatSessionManager.java`

Purpose:

- Find the authenticated channel’s currently active broadcast.
- Obtain:
  - broadcast ID
  - `snippet.liveChatId`

Frequency when an active livestream exists:

- Normally once after application startup.
- Repeated only after session invalidation.

Frequency when no active livestream exists:

- Once every configured retry interval.
- Default retry interval: 30 seconds.
- Therefore up to:
  - 2 requests/minute
  - 120 requests/hour
  - 480 requests over four hours

Additional invocation path:

- `YouTubePublisher.publish()` calls `sessionManager.currentSession()`.
- If no session is cached, publishing can trigger another discovery.
- This matters when Discord messages produce replies while no YouTube livestream is active.

The API documentation confirms that `snippet.liveChatId` is the identifier used by live-chat message methods: <https://developers.google.com/youtube/v3/live/docs/liveBroadcasts>

## 2. `liveChatMessages.list`

HTTP request:

```http
GET https://www.googleapis.com/youtube/v3/liveChat/messages
```

Parameters:

```text
liveChatId=<cached live chat ID>
part=snippet,authorDetails
pageToken=<previous nextPageToken>
```

Called from:

- `YouTubePlatformAdapter.requestMessages()`

Class:

- `YouTubePlatformAdapter.java`

Purpose:

- Retrieve new live-chat messages.
- Retrieve:
  - message text and type
  - publication timestamp
  - author channel ID
  - author display name
  - author role information
  - continuation token
  - recommended polling interval

Frequency:

```text
60 / max(YouTube polling interval, configured minimum interval)
```

The configured minimum is 10 seconds.

| Effective interval | Requests/minute | Requests/hour | Four hours |
|---:|---:|---:|---:|
| 10 seconds | 6 | 360 | 1,440 |
| 15 seconds | 4 | 240 | 960 |
| 20 seconds | 3 | 180 | 720 |
| 30 seconds | 2 | 120 | 480 |

The code correctly sends the previous response’s `nextPageToken` on the following request.

The first returned page is intentionally not published into the application. This avoids processing historical messages after startup, although the API request itself still consumes quota.

YouTube now recommends `liveChatMessages.streamList` instead of repeated `list` polling because streaming pushes messages and reduces quota pressure:

- <https://developers.google.com/youtube/v3/live/docs/liveChatMessages/list>
- <https://developers.google.com/youtube/v3/live/docs/liveChatMessages/streamList>

## 3. `channels.list`

HTTP request:

```http
GET https://www.googleapis.com/youtube/v3/channels
```

Parameters:

```text
part=snippet
id=<author channel ID>
```

Called from:

- `YouTubePlatformAdapter.fetchAuthorIdentity()`

Class:

- `YouTubePlatformAdapter.java`

Purpose:

- Enrich live-chat `authorDetails` with:
  - channel `customUrl`, interpreted as a handle when it begins with `@`
  - channel title, used as display name

Frequency:

- Once per unique YouTube channel ID per running JVM.
- The result is cached in `authorIdentityCache`.
- Restarting ChatBrain clears the cache and causes the same viewers to be looked up again.

| Unique authors | `channels.list` requests |
|---:|---:|
| 10 | 10 |
| 100 | 100 |
| 500 | 500 |
| 1,000 | 1,000 |

Official quota cost:

- 1 unit per call: <https://developers.google.com/youtube/v3/docs/channels/list>

The API permits multiple channel IDs in one call, but ChatBrain currently sends one ID per request.

## 4. `liveChatMessages.insert`

HTTP request:

```http
POST https://www.googleapis.com/youtube/v3/liveChat/messages
```

Parameters:

```text
part=snippet
```

Request body:

```json
{
  "snippet": {
    "liveChatId": "...",
    "type": "textMessageEvent",
    "textMessageDetails": {
      "messageText": "..."
    }
  }
}
```

Called from:

- `YouTubePublisher.publish()`

Class:

- `YouTubePublisher.java`

Purpose:

- Publish the final ChatBrain response into YouTube Live Chat.

Frequency:

- Once per AI `REPLY` decision.
- No call for `IGNORE`.
- YouTube messages and Discord messages can both produce YouTube replies.
- Frequency therefore depends on total cross-platform input volume and AI decisions.

If the AI chooses one reply per minute:

```text
1 request/minute
60 requests/hour
240 requests/four-hour stream
```

The endpoint requires the `youtube.force-ssl` or `youtube` scope, and ChatBrain uses `youtube.force-ssl`: <https://developers.google.com/youtube/v3/live/docs/liveChatMessages/insert>

## OAuth requests

OAuth itself is not a YouTube Data API quota operation.

ChatBrain uses:

- Google OAuth authorization endpoint during initial consent
- OAuth token endpoint for access-token refresh

Configuration:

- OAuth scope: `youtube.force-ssl`
- access type: offline
- token storage: `data/tokens/` by default
- OAuth callback port: `8888`

Class:

- `YouTubeConfiguration.java`

Refreshing an OAuth access token should not consume YouTube Data API query quota.

---

# 2. Complete call flow

## Application startup

```text
Spring Boot starts
        ↓
YouTubeConfiguration loads client-secret.json
        ↓
Stored OAuth credential is loaded from data/tokens/
        ↓
If necessary, OAuth access token is refreshed
        ↓
YouTube API client bean is created
        ↓
ApplicationReadyEvent
        ↓
YouTubePlatformAdapter.start()
        ↓
Dedicated youtube-live-chat-poller thread starts
```

## Session discovery

```text
pollActiveLiveChat()
        ↓
LiveChatSessionManager.currentSession()
        ↓
No cached session
        ↓
GET liveBroadcasts.list
        ↓
Read broadcastId + snippet.liveChatId
        ↓
Cache LiveChatSession
```

If no active broadcast is found:

```text
Wait 30 seconds
        ↓
GET liveBroadcasts.list again
        ↓
Repeat indefinitely while application runs
```

## Chat polling

```text
GET liveChatMessages.list
  pageToken = null
        ↓
Initial history page is discarded
        ↓
Save nextPageToken
        ↓
Wait max(pollingIntervalMillis, 10 seconds)
        ↓
GET liveChatMessages.list
  pageToken = previous nextPageToken
        ↓
Process new messages
        ↓
Repeat
```

## Author enrichment

For each newly encountered channel ID:

```text
Check authorIdentityCache
        ↓
Cache miss
        ↓
GET channels.list
        ↓
Read customUrl + channel title
        ↓
Cache identity for JVM lifetime
```

For returning authors in the same process:

```text
Check authorIdentityCache
        ↓
Cache hit
        ↓
No API request
```

## Event and AI processing

```text
PlatformMessage
        ↓
ChatMessageEvent
        ↓
IdentityResolver
        ↓
MemoryRetriever
        ↓
PromptBuilder
        ↓
OpenAI or FakeLLMClient
        ↓
AIResponseDecision
```

## `REPLY`

```text
AI action = REPLY
        ↓
YouTubePublisher.publish()
        ↓
LiveChatSessionManager.currentSession()
        ↓
Cached session exists: no discovery request
        ↓
POST liveChatMessages.insert
        ↓
Remember published message ID and response text
```

## `IGNORE`

```text
AI action = IGNORE
        ↓
No YouTube API publication request
```

## Loop prevention

```text
liveChatMessages.list returns ChatBrain message
        ↓
Check published message ID and response text caches
        ↓
Recognized as ChatBrain output
        ↓
Message discarded
        ↓
No AI loop
```

## Session termination

A session is invalidated when:

- a `chatEndedEvent` appears
- the API returns:
  - `liveChatDisabled`
  - `liveChatEnded`
  - `liveChatNotFound`

Then:

```text
Invalidate cached LiveChatSession
        ↓
Exit message-polling loop
        ↓
Wait 30 seconds
        ↓
Call liveBroadcasts.list again
```

---

# 3. Quota analysis

## Important quota-cost uncertainty

The current official quota calculator states that:

- all requests cost at least one unit
- Live Streaming API calls consume YouTube Data API quota
- the default shared daily allocation is 10,000 units

However, the current published table does not expose individual rows for `liveBroadcasts` or `liveChatMessages`. It explicitly confirms `channels.list = 1`, but not the three live methods used by ChatBrain: <https://developers.google.com/youtube/v3/determine_quota_cost>

Therefore this report uses two models.

### Confirmed-minimum model

- Every request: at least 1 unit
- `channels.list`: exactly 1 unit

### Conservative legacy planning model

Historically used estimates:

- `liveBroadcasts.list`: 1 unit
- `liveChatMessages.list`: 5 units
- `liveChatMessages.insert`: 50 units
- `channels.list`: 1 unit

These legacy live-method costs should not be treated as confirmed current pricing. The Google Cloud Console’s per-method quota metrics are the authoritative measurement for your project.

## Active-stream baseline

Variables:

```text
P = effective polling interval in seconds
U = unique YouTube authors during the stream
R = AI replies per minute
```

Requests during a four-hour stream:

```text
liveBroadcasts.list ≈ 1
liveChatMessages.list = 14,400 / P
channels.list = U
liveChatMessages.insert = 240 × R
```

At the current maximum polling rate, `P = 10`:

```text
liveChatMessages.list = 1,440
```

## Per-endpoint estimates

| Endpoint | Assumed cost | Requests/min | Requests/hour | Four-hour requests | Four-hour quota |
|---|---:|---:|---:|---:|---:|
| `liveBroadcasts.list` with active cache | 1 assumed | near 0 | near 0 | 1 | 1 |
| `liveChatMessages.list` | minimum 1 | up to 6 | up to 360 | up to 1,440 | at least 1,440 |
| `liveChatMessages.list` | legacy 5 | up to 6 | up to 360 | up to 1,440 | 7,200 |
| `channels.list` | confirmed 1 | viewer-dependent | viewer-dependent | `U` | `U` |
| `liveChatMessages.insert` | minimum 1 | `R` | `60R` | `240R` | `240R` |
| `liveChatMessages.insert` | legacy 50 | `R` | `60R` | `240R` | `12,000R` |

## Example: one reply per minute, 40 unique authors

Requests:

```text
Broadcast discovery:       1
Chat polling:           1,440
Channel enrichment:        40
Reply insertion:           240
```

Confirmed-minimum quota:

```text
1 + 1,440 + 40 + 240 = 1,721 units
```

Conservative legacy estimate:

```text
1 + 7,200 + 40 + 12,000 = 19,241 units
```

The large difference demonstrates why actual Cloud Console per-method measurements must be checked before assuming the current cost model.

## No active livestream

With the application running for four hours:

```text
30-second retry interval
2 discoveries/minute
120 discoveries/hour
480 discoveries/four hours
```

Minimum/assumed cost:

```text
480 quota units
```

This can be higher if Discord messages trigger YouTube publication attempts, because `YouTubePublisher` also asks `LiveChatSessionManager` for a session and an empty session is not negatively cached.

---

# 4. Polling analysis

## Current interval

Configuration:

```properties
chatbrain.youtube.minimum-poll-interval=10s
chatbrain.youtube.retry-interval=30s
```

The polling policy calculates:

```java
max(youtubePollingIntervalMillis, configuredMinimum)
```

Behavior:

- YouTube says 5 seconds → ChatBrain waits 10 seconds.
- YouTube says 10 seconds → waits 10 seconds.
- YouTube says 15 seconds → waits 15 seconds.
- YouTube omits the interval → waits 10 seconds.

## `pollingIntervalMillis`

The value from every response is used before the next request.

This complies with the documentation’s requirement that clients wait at least the returned interval. YouTube warns that polling faster can cause `rateLimitExceeded`: <https://developers.google.com/youtube/v3/live/docs/liveChatMessages/list>

## `nextPageToken`

ChatBrain correctly:

1. starts without a token
2. receives a `nextPageToken`
3. uses that token on the next call
4. replaces it with the next response’s token

This prevents repeatedly retrieving the same page during a valid session.

## Is unnecessary polling occurring?

Yes, structurally:

- REST polling happens even when chat is silent.
- At a 10-second interval, a silent four-hour stream can still produce 1,440 list calls.
- The initial page is fetched and then discarded.
- Polling continues as long as the session appears valid.
- The implementation does not use the newer push-based `streamList`.

However, within the current REST polling design, the implementation is no longer polling faster than YouTube recommends.

## Can polling safely be reduced?

Options, without implementing them yet:

1. Increase the configured minimum to 15–30 seconds.
   - Easy.
   - Reduces quota.
   - Adds reply latency.

2. Use `streamList`.
   - Officially recommended.
   - Eliminates periodic polling during silent periods.
   - Preserves low latency.
   - Requires a more substantial transport/lifecycle change.

The strongest long-term solution is `streamList`, not simply a larger fixed delay.

---

# 5. Caching audit

## LiveChatSession

Contains:

- broadcast ID
- live-chat ID
- discovery timestamp

Location:

- `LiveChatSessionManager.activeSession`

Lifetime:

- in-memory for the current application process

Expiration:

- no time-based expiration

Invalidated when:

- `chatEndedEvent` is received
- polling returns a known invalid-session error
- explicitly calling `invalidate()`

Refresh:

- rediscovered only when no active session exists

Assessment:

- Good for quota reduction during an active session.
- `discoveredAt` is stored but not used.
- A stale session can remain until polling identifies it as invalid.
- No empty-result cache exists.

## Author identity cache

Contains:

- handle
- display name

Key:

- YouTube channel ID

Location:

- `YouTubePlatformAdapter.authorIdentityCache`

Implementation:

- `ConcurrentHashMap`

Lifetime:

- entire application process

Expiration:

- none

Refresh:

- never during the process

Assessment:

- Effective at preventing repeated channel lookups.
- Lost on every restart.
- No maximum size.
- A temporary failed channel lookup stores fallback data permanently for that process.
- Handle or channel-title changes are not refreshed.

## OAuth tokens

Location:

```text
data/tokens/
```

Default override:

```text
YOUTUBE_TOKEN_PATH
```

Lifetime:

- persisted across restarts

Refresh:

- Google OAuth client refreshes access credentials using the stored refresh token

Assessment:

- Appropriate.
- OAuth refresh does not require repeating user consent.
- Does not consume Data API quota.

## Published message IDs

Location:

- `YouTubePublisher.publishedMessageIds`

Purpose:

- identify ChatBrain’s own inserted messages

Lifetime:

- removed when observed in the polling feed
- no TTL for IDs that are never observed

Assessment:

- Prevents loops and therefore prevents potentially catastrophic recursive API usage.
- IDs can remain indefinitely if a published message never appears in polling results.

## Published response text

Location:

- `YouTubePublisher.publishedResponses`

Expiration:

- 10 minutes

Cleanup:

- expired entries are removed only when another message is published

Assessment:

- secondary loop protection
- identical response text uses the text as the map key, so repeated identical replies overwrite one another
- the first matching polled message removes the entry

## Continuation token

Location:

- local `nextPageToken` variable inside `pollMessages()`

Lifetime:

- one active polling session

Reset:

- adapter restart
- session invalidation
- reconnection through a newly entered `pollMessages()`

Assessment:

- Correct during a stable session.
- Not persisted across process restarts.
- The initial page after restart is discarded, reducing duplicate event processing.

---

# 6. Redundant request audit

## Repeated channel lookups across restarts

Current behavior:

- One lookup per unique author per JVM.
- Restarting clears the cache.

Impact:

- Regular development restarts repeatedly enrich the same viewers.
- Production restarts repeat all lookups for returning viewers.

Opportunity:

- Persist handle/display-name enrichment using `PlatformIdentity`.
- Refresh only when missing or stale.

## One channel per `channels.list` request

Current behavior:

```text
1 unique author → 1 API request
```

The endpoint supports multiple channel IDs in one request.

Opportunity:

- Batch channel enrichment.
- Requires buffering messages or asynchronous enrichment.

## Discovery while no broadcast is active

Current behavior:

- `liveBroadcasts.list` every 30 seconds indefinitely.

Opportunity:

- exponential backoff
- a longer idle-discovery interval
- scheduled stream awareness
- explicit “stream active” control

## Publisher can independently trigger discovery

Current behavior:

```text
YouTubePublisher.publish()
    → currentSession()
    → discoverSession() when empty
```

Potential duplication:

- Polling loop already performs discovery.
- Discord messages can cause additional discoveries between polling retries.
- Synchronization prevents simultaneous duplicate discovery, but empty results are not cached.

Opportunity:

- Make publishing consume only an already-established active session.
- Keep discovery owned exclusively by the session-management/polling lifecycle.

## Failed publication against stale session

Current behavior:

- `liveChatMessages.insert` errors are logged.
- Publisher does not invalidate the session on `liveChatEnded`, `liveChatDisabled`, or `liveChatNotFound`.

Impact:

- Subsequent replies may repeatedly attempt inserts using the same stale session until the polling thread invalidates it.

Opportunity:

- Apply the same invalid-session classification used by the adapter.

## Identity enrichment may be unnecessary for every message

`authorDetails` already contains:

- permanent channel ID
- visible display name

The additional `channels.list` call exists primarily to separate:

- handle/custom URL
- channel title/display name

Opportunity:

- Use live-chat author data immediately.
- Enrich the handle lazily or only when identity-related behavior requires it.

## Initial history page

Current behavior:

- First `liveChatMessages.list` call is mandatory to obtain the continuation token.
- Returned history is discarded.

Assessment:

- This request is not avoidable in the REST polling model.
- `streamList` also returns initial history but avoids subsequent periodic polling.

---

# 7. Top 10 quota hotspots

## 1. REST chat polling

Current behavior:

- Up to 1,440 list calls in four hours.

Why inefficient:

- Requests continue during silent chat periods.

Potential optimization:

- Replace polling with `liveChatMessages.streamList`.

Estimated impact:

- Highest read-side saving.
- Could eliminate most periodic chat-list requests.
- Also improves latency.

## 2. Reply insertion volume

Current behavior:

- Every AI `REPLY` creates one insert request.
- Discord replies are also posted to YouTube.

Why inefficient:

- An overly eager AI can create more write traffic than necessary.
- Under historical quota assumptions, insert operations are expensive.

Potential optimization:

- Tune the AI decision prompt and policy toward selective replies.
- Add response rate limits later if necessary.

Estimated impact:

- Potentially the largest overall saving when chat is active.

## 3. Publisher-triggered discovery

Current behavior:

- Publishing calls `currentSession()`, which may make a discovery request.

Why inefficient:

- Output traffic can trigger discovery even when the polling lifecycle already knows no stream is active.

Potential optimization:

- Require an already-active cached session for publication.

Estimated impact:

- High when Discord is active while YouTube is offline.
- Low during a stable livestream.

## 4. Fixed 30-second empty-session discovery

Current behavior:

- 480 calls over four idle hours.

Why inefficient:

- An inactive application can consume quota without processing messages.

Potential optimization:

- progressive backoff or explicit stream schedule/activation.

Estimated impact:

- Up to approximately 480 calls over four hours of idle operation.

## 5. Per-author `channels.list`

Current behavior:

- One call per unique viewer per process.

Why inefficient:

- Most required identity information already exists in `authorDetails`.

Potential optimization:

- delay handle enrichment
- accept missing handles
- enrich only when required

Estimated impact:

- Saves approximately one call per unique viewer.

## 6. Nonpersistent author cache

Current behavior:

- Every restart repeats channel enrichment.

Why inefficient:

- ChatBrain already persists `PlatformIdentity`.

Potential optimization:

- reuse persisted handle/display-name data with a refresh policy.

Estimated impact:

- High during development or frequent deployments.
- Moderate in stable production.

## 7. No batching of channel lookups

Current behavior:

- One channel ID per request.

Why inefficient:

- `channels.list` supports multiple IDs.

Potential optimization:

- batch unresolved authors.

Estimated impact:

- Up to roughly 50:1 reduction in enrichment calls in high-volume chat, depending on API limits and batching latency.

## 8. Stale-session insert retries

Current behavior:

- Publisher does not invalidate a session after terminal insert failures.

Why inefficient:

- Multiple replies may repeat known-doomed write requests.

Potential optimization:

- classify terminal insert errors and invalidate the session.

Estimated impact:

- Usually small, but substantial during the window immediately after stream termination.

## 9. Fixed retry delay for all API errors

Current behavior:

- Every polling/discovery error retries after 30 seconds.

Why inefficient:

- Quota exhaustion, permission errors, transient server errors, and network failures receive the same treatment.

Potential optimization:

- error-specific backoff
- exponential delay
- honor `Retry-After`
- daily quota circuit breaker

Estimated impact:

- High during outages or quota exhaustion.
- Minimal during normal operation.

## 10. Limited quota observability

Current behavior:

- Only `liveChatMessages.list` attempted-request count is logged.
- Broadcast, channel, and insert request counts are not tracked together.

Why inefficient:

- Quota regressions cannot be attributed to a specific method from application telemetry.

Potential optimization:

- per-method counters
- response/error counters
- estimated quota budget
- Actuator metrics

Estimated impact:

- No direct savings.
- High diagnostic and operational value.

---

# 8. Architecture review

## Separation of concerns

Strengths:

- `YouTubeConfiguration` owns OAuth/client creation.
- `LiveChatSessionManager` owns discovery and session caching.
- `YouTubePlatformAdapter` owns input polling and mapping.
- `YouTubePublisher` owns output.
- `YouTubePollingPolicy` owns interval calculation.
- Downstream AI and memory code are platform-neutral.

Assessment:

- Good overall separation.
- `YouTubePlatformAdapter` is still relatively large because it owns polling, message filtering, identity enrichment, event mapping, logging, error handling, and author caching.

## API abstraction

Strengths:

- Application code downstream of `ChatMessageEvent` does not depend on Google classes.
- `YouTubePublisher` encapsulates output logic.

Weaknesses:

- Google `YouTube` client is injected directly into several components.
- There is no narrow internal gateway around Data API operations.
- Unit tests must mock the generated Google API client structure.

Scaling concern:

- Quota policies, observability, retry classification, and request deduplication are harder to enforce consistently across direct callers.

## Caching strategy

Strengths:

- Active session is cached.
- Author identities are cached.
- OAuth tokens persist.
- Published replies are tracked to prevent loops.

Weaknesses:

- Empty session results are not cached.
- Author cache is not persisted or bounded.
- Failed identity enrichment is cached forever.
- Session discovery timestamp is unused.
- Publication cache cleanup is partial.

## Retry strategy

Current behavior:

- Fixed 30-second delay.
- No exponential backoff.
- No jitter.
- No maximum retry interval.
- No `Retry-After` handling.
- No quota-exhaustion circuit breaker.

Assessment:

- Simple and predictable.
- Insufficient for sustained production failures or multiple application instances.

## Resilience

Strengths:

- Temporary polling network errors do not terminate the application.
- Known terminal chat errors invalidate the session.
- polling executor is isolated from the main application thread.
- loop prevention protects against recursive AI replies.

Weaknesses:

- Synchronous Spring event processing means database, OpenAI, and YouTube publication happen on the polling thread.
- A slow OpenAI request delays the next poll.
- Publication failure is swallowed, and the orchestrator cannot distinguish success from failure.
- Terminal publication failures do not invalidate the session.

## Error handling

Strengths:

- Google JSON errors and IO failures are logged separately.
- Known invalid-session reasons are classified during polling.
- OAuth startup failures are explicit.

Weaknesses:

- Error response classification is duplicated/incomplete.
- Generic 403 errors can represent permissions, rate limits, or quota exhaustion.
- Logs state that operations will be retried even when the error may be permanent.
- Channel enrichment catches all IO failures and permanently caches fallback identity.

## Thread safety

Strengths:

- `LiveChatSessionManager` methods are synchronized.
- author and publication caches use concurrent collections.
- adapter startup uses atomic running state and synchronized lifecycle methods.

Concerns:

- Discord and YouTube event threads can concurrently call `YouTubePublisher`.
- Session access is safe, but quota/request scheduling is not globally coordinated.
- Multiple application instances would each run their own discovery and polling loops.
- All caches are process-local.

## Quota awareness

Implemented:

- 10-second minimum interval
- respects YouTube’s returned interval
- session cache
- author cache
- attempted chat-list request counter
- loop prevention

Missing:

- `streamList`
- total per-endpoint request accounting
- quota budget
- per-day circuit breaker
- adaptive idle discovery
- response rate limiting
- multi-instance coordination
- quota-specific error handling

---

# 9. Ranked recommendations

| Rank | Recommendation | Effort | Expected quota savings | Architectural impact | Timing |
|---:|---|---|---|---|---|
| 1 | Replace REST polling with `liveChatMessages.streamList` | High | Very high; removes most periodic list calls | Moderate, contained to YouTube input/session lifecycle | Implement next after a focused design |
| 2 | Measure actual per-method Cloud Console usage before assuming costs | Low | Indirect but essential | None | Now |
| 3 | Keep AI response decisions selective and measure reply rate | Low–Medium | Potentially very high on insert traffic | No architecture change | Now |
| 4 | Prevent `YouTubePublisher` from discovering sessions when none is active | Low–Medium | High while YouTube is offline and Discord is active | Small session-contract refinement | Soon |
| 5 | Add exponential idle discovery backoff | Low | Up to hundreds of calls per idle period | Small polling-policy change | Soon |
| 6 | Reuse persisted `PlatformIdentity` enrichment across restarts | Medium | One call per returning unique viewer | Touches identity enrichment boundaries | Soon |
| 7 | Invalidate session on terminal insert failures | Low | Prevents repeated failed writes | Small shared error-classification improvement | Now |
| 8 | Make channel/handle enrichment optional or lazy | Medium | Up to one call per unique viewer | May affect identity completeness timing | Soon |
| 9 | Batch unresolved channel lookups | Medium–High | Up to large reductions in high-volume chat | Adds buffering/asynchronous enrichment | Later, only if needed |
| 10 | Add per-method metrics and quota alarms | Medium | Indirect | Adds operational observability, not business logic | Soon |
| 11 | Add quota-aware circuit breaking and error-specific retries | Medium | High during failures | Centralizes YouTube request policy | Before production scaling |
| 12 | Coordinate polling across multiple instances | High | Prevents multiplied quota usage | Requires deployment-level ownership/locking | When horizontal scaling begins |

## Recommended immediate decision sequence

Before changing implementation:

1. Run one controlled 10-minute livestream.
2. Record Cloud Console usage before and after.
3. Record ChatBrain’s logged `liveChatMessages.list` request count.
4. Count:
   - unique authors
   - published replies
   - broadcast discoveries
5. Compare the quota delta with expected request volume.
6. Determine the current effective cost of:
   - `liveChatMessages.list`
   - `liveChatMessages.insert`
7. Then decide whether to prioritize:
   - `streamList`
   - response-volume reduction
   - channel-enrichment reduction

## Final assessment

The previous session-management and 10-second polling changes have eliminated the most obvious runaway loop. During a valid active session, broadcast discovery is cached and polling does not rediscover the broadcast.

The remaining structural quota hotspot is `liveChatMessages.list`: even a silent stream can generate up to 1,440 requests over four hours. YouTube’s current documentation explicitly identifies `streamList` as the most efficient replacement.

The other potentially dominant cost is response publication. Its significance depends on the current quota cost of `liveChatMessages.insert` and the proportion of messages receiving `REPLY`. The newly introduced AI decision model is therefore directly relevant: effective `IGNORE` decisions can reduce output quota without changing the YouTube integration itself.
