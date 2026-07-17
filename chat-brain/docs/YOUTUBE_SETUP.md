# YouTube Live Chat Setup

This guide configures ChatBrain to read messages from an active livestream owned by the authenticated YouTube account and send one hardcoded verification reply.

## 1. Create a Google Cloud project

1. Open the [Google Cloud Console](https://console.cloud.google.com/).
2. Use the project selector to create a project, for example `ChatBrain`.
3. Confirm that the new project is selected before configuring APIs or credentials.

## 2. Enable YouTube Data API v3

1. Open **APIs & Services → Library**.
2. Search for **YouTube Data API v3**.
3. Open it and select **Enable**.

No other Google API is required for live-chat reading.

## 3. Configure the OAuth consent screen

1. Open **Google Auth Platform → Branding** and enter the application name and support email.
2. Select **External** unless the application belongs to a Google Workspace organization and should be internal.
3. Complete the required contact information.
4. Keep the application in **Testing** while developing.
5. Add the Google account that owns the YouTube channel under **Audience → Test users**.

ChatBrain requests this scope so it can read live chat and send the hardcoded reply:

```text
https://www.googleapis.com/auth/youtube.force-ssl
```

## 4. Create an OAuth Desktop client

1. Open **Google Auth Platform → Clients**.
2. Select **Create client**.
3. Choose **Desktop app** as the application type.
4. Name it `ChatBrain Desktop` and create it.
5. Download the OAuth client JSON.

Do not create a Web application client for this setup. ChatBrain uses the installed-application loopback authorization flow.

## 5. Place and rename the credentials JSON

The downloaded filename is normally similar to:

```text
client_secret_123456789.apps.googleusercontent.com.json
```

Rename it to:

```text
client-secret.json
```

### Default location

Place the renamed file at this path relative to the Maven project root:

```text
config/youtube/client-secret.json
```

The default path is resolved from the directory in which ChatBrain is started. Run ChatBrain from the Maven project root so the default resolves correctly.

The credentials path can be overridden when necessary:

```bash
YOUTUBE_ENABLED=true \
YOUTUBE_CREDENTIALS_PATH=/secure/location/client-secret.json \
./mvnw spring-boot:run
```

Add the credential file and token directory to the applicable `.gitignore` before starting ChatBrain:

```gitignore
/chat-brain/config/youtube/client-secret.json
/chat-brain/data/tokens/
```

The `/chat-brain/` prefix is required because this checkout's repository-level `.gitignore` is one directory above the Maven project. If `.gitignore` is located directly in the Maven project root, use `/config/youtube/client-secret.json` and `/data/tokens/` instead.

Never commit the credential JSON or generated OAuth tokens.

## 6. Token storage

By default, OAuth access and refresh token data is stored relative to the Maven project root at:

```text
data/tokens/
```

Override it with `YOUTUBE_TOKEN_PATH` if necessary. The stored refresh token allows later starts to authenticate without repeating consent while authorization remains valid.

## 7. Prepare the livestream

1. Sign in to the YouTube channel using the same Google account that will authorize ChatBrain.
2. Create or start a livestream.
3. Ensure live chat is enabled.
4. Wait until the broadcast status is active. A scheduled or completed broadcast is not selected.

## 8. First application startup and OAuth flow

With credentials in the default location, run from the Maven project root:

```bash
YOUTUBE_ENABLED=true ./mvnw spring-boot:run
```

During the first startup:

1. ChatBrain reads `client-secret.json`.
2. A browser opens the Google authorization page.
3. Sign in using the account that owns the active broadcast.
4. Select **Allow** for YouTube access. The write-capable scope is required only for the hardcoded Day 1 reply.
5. Google redirects the browser to ChatBrain's loopback receiver on port `8888`.
6. ChatBrain stores the OAuth token and initializes the YouTube API client.
7. After application startup, the adapter finds the active broadcast and begins reading live chat.

If port `8888` is unavailable, select another callback port:

```bash
YOUTUBE_ENABLED=true YOUTUBE_OAUTH_PORT=8889 ./mvnw spring-boot:run
```

## 9. Expected console logs

With YouTube enabled and an active broadcast, expect:

```text
Starting YouTube adapter...
Connected to active YouTube livestream chat
```

After typing `Hello ChatBrain` in live chat, expect the identity log:

```text
Platform : YOUTUBE
Platform         : YOUTUBE
Platform User ID : <stable YouTube channel ID>
Handle           : null
Display Name     : <exact author display name returned by YouTube>
Message          : Hello ChatBrain
Timestamp        : <message timestamp>
```

ChatBrain keeps these YouTube values separate:

- **Platform User ID** is the platform-neutral identity key. For YouTube, it contains the permanent author channel ID returned by `authorDetails.channelId`.
- **Handle** is a nullable public platform handle. YouTube Live Chat `authorDetails` does not expose one separately, so ChatBrain performs one cached `channels.list(part=snippet)` lookup per encountered channel. It accepts `snippet.customUrl` only when YouTube explicitly returns an `@...` value; otherwise the handle remains `null`.
- **Display Name** uses the channel title from `channels.list` when available and falls back to the exact `authorDetails.displayName` value returned with the live-chat message. ChatBrain treats it as mutable presentation data and never parses it to manufacture a handle.
- **Real Name** belongs to ChatBrain's platform-independent user. It remains nullable and is never inferred automatically from the display name.

Real names may only be learned later through the Identity Resolver or Memory Engine when explicitly supported.

The first API response establishes the page cursor and is treated as existing chat history. Send the verification message after `Connected to active YouTube livestream chat` appears.

## 10. Troubleshooting

### YouTube adapter does not start

Confirm that `YOUTUBE_ENABLED=true` is present in the environment for the same command that starts Spring Boot. YouTube integration is disabled by default.

### Credentials file not found

The error includes the resolved path. Confirm the file exists, is readable, and is named `client-secret.json`. If it is elsewhere, set `YOUTUBE_CREDENTIALS_PATH` to its absolute path.

### Browser does not open

Copy the authorization URL from the terminal and open it manually. Ensure a graphical browser is available on the machine running ChatBrain.

### Redirect or local receiver fails

Port `8888` may already be in use. Set `YOUTUBE_OAUTH_PORT` to another free port and restart. Use a Desktop OAuth client, which supports installed-application loopback redirects.

### Access blocked or app not verified

While the OAuth app is in Testing, add the authorizing account as a test user. Confirm the consent screen and OAuth client belong to the same Google Cloud project.

### No active livestream found

Confirm the authenticated account owns the livestream, the broadcast is currently active, and live chat is enabled. Scheduled streams are not returned by the adapter's active-broadcast query.

### Live chat polling returns 403

Confirm YouTube Data API v3 is enabled, the authenticated account can access the broadcast, and live chat is enabled. If authorization was granted using the wrong account, delete the token directory and restart to authorize again.

### Quota or rate-limit errors

Check the YouTube Data API v3 quota in Google Cloud Console. ChatBrain waits for the polling interval returned by YouTube; do not run multiple instances against the same stream during verification.

### Authorization changed but old token is reused

Stop ChatBrain, delete `data/tokens/`, and restart with `YOUTUBE_ENABLED=true` to repeat OAuth consent. If `YOUTUBE_TOKEN_PATH` is set, delete that configured token directory instead.

## Final verification checklist

- [ ] Google Cloud project created and selected
- [ ] YouTube Data API v3 enabled
- [ ] OAuth consent screen configured
- [ ] Channel owner added as a test user
- [ ] Desktop OAuth client created
- [ ] Credentials JSON downloaded
- [ ] File renamed to `client-secret.json`
- [ ] Credentials placed at the configured path
- [ ] Credentials and tokens excluded from Git if stored in the project
- [ ] Livestream active with live chat enabled
- [ ] ChatBrain started with `YOUTUBE_ENABLED=true`
- [ ] Browser authorization page opened
- [ ] Correct channel owner account selected
- [ ] YouTube permission granted
- [ ] Token created in the configured token directory
- [ ] `Connected to active YouTube livestream chat` logged
- [ ] `hello bot` sent after the connected log
- [ ] `PlatformMessage` details logged
- [ ] Existing `ChatMessageListener` received `ChatMessageEvent`
- [ ] `Hello from ChatBrain 👋` posted exactly once
