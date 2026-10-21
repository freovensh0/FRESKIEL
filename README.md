# FRESKIEL

An Android client and assistant surface for the [Pi coding agent](https://pi.dev). It connects to a
self-hosted bridge that runs `pi --mode rpc` on a machine that already has the sessions.

Forked from [ayagmar/pi-mobile](https://github.com/ayagmar/pi-mobile) (MIT), rebranded, and extended
towards the Android default-assistant role: long-press home, talk or type, and hand the agent a
screenshot, a photo or a file.

## How it works

```
Android app  --WebSocket + bearer token-->  bridge (Node)  -->  pi --mode rpc  -->  ~/.pi/agent/sessions
```

The bridge owns process lifetime, cwd locks and session indexing. The app owns the reading and
composing surface. One Pi process runs per working directory.

## Setup

On the machine that runs Pi:

```sh
npm install -g @earendil-works/pi-coding-agent
pi --version                     # 0.80.6 or newer

cd bridge
pnpm install
cp .env.example .env             # set BRIDGE_AUTH_TOKEN
pnpm start
```

Pair the phone with `pnpm pair` (QR) or enter the host, port and token by hand. The bridge binds to
`127.0.0.1:8787`; expose it over Tailscale or TLS, never open it to the internet unauthenticated.

The APK is built by GitHub Actions on every push:

**Actions → ci → Artifacts → freskiel-debug**

```sh
gh run download -n freskiel-debug -D /tmp/freskiel
adb install -r /tmp/freskiel/app-debug.apk
```

The development machine does not need an Android SDK; CI is the build machine.

## Signing

Every build is signed with one key, so a new APK installs over the previous one without a
uninstall. The key lives outside the repository:

- CI reads it from the repository secrets `FRESKIEL_KEYSTORE_BASE64`, `FRESKIEL_KEYSTORE_PASSWORD`,
  `FRESKIEL_KEY_ALIAS` and `FRESKIEL_KEY_PASSWORD`.
- A local build reads the same values from `FRESKIEL_KEYSTORE_PATH` and friends, or from
  `freskiel.keystorePath` in `~/.gradle/gradle.properties`.

The first build with this key needs the old app removed once, because the previous debug builds
were signed with the throwaway key of the build machine.

## Assistant role

Set FRESKIEL under **Settings → Apps → Default apps → Digital assistant app**. The assist gesture
(long-press home, or the gesture your ROM uses) asks the system for the focused screen, stores one
screenshot in the app cache, and opens the composer with the screen text and the screenshot attached.

- Screenshots are taken only after the assist gesture; nothing is captured in the background.
- The shot is written to `cache/shared-images/` and removed when the composer sends or the app clears
  its cache.
- If no chat is open, the app resumes the session that was used last and opens its composer. When
  there is none, the app lands on the session list; opening a session applies the pending text and
  shot to that composer.
- Photo attachment uses the system photo picker. Generic file attachment needs an upload endpoint on
  the bridge and is not wired yet.

## What works today

- Session list, resume, fork/tree navigation, model picker, slash commands.
- Streaming answers, thinking blocks, tool output with collapse/expand.
- Image attachments from the photo picker and shared files.
- File attachments: the phone uploads the file to the bridge machine and the prompt carries the
  path the agent reads. Files up to 10 MB.
- Host profiles, QR pairing, token storage in the Android Keystore.
- Dark and light themes.

## Layout

```
app/            Compose UI, view models, assistant surface
core-rpc/       typed Pi RPC commands and events
core-net/       WebSocket transport, auth, reconnect
core-sessions/  session index models and repositories
bridge/         Node bridge, auth, process and cwd locks
docs/           architecture, protocol, testing, release
```

## License

MIT. See `LICENSE` (upstream copyright) and `NOTICE`.
