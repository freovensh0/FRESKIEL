# Working on FRESKIEL

FRESKIEL is an Android client and assistant surface for the Pi coding agent. The phone talks to a
self-hosted bridge; the bridge owns Pi processes and sessions. This file is the contract for humans
and agents changing the repository.

## Language

The repository is **English**: code, identifiers, comments, docs, commit messages, CI step names and
UI strings. A Turkish string is allowed only when a task explicitly asks for one, and then the
English string stays next to it.

## Architecture invariants

- Android → authenticated WebSocket bridge → one isolated `pi --mode rpc` process per working
  directory. Do not bypass the bridge, spawn Pi on the device, or migrate the bridge to the Pi SDK.
- Use documented RPC commands and events only. An unknown session entry triggers one full rebuild;
  never guess private session-file behaviour.
- The assistant layer is not a second app. `VoiceInteractionService` stays lightweight and delegates
  UI to the existing composer. No session state lives in the service.
- Assist screenshots and text are read only after an explicit assist gesture. They go to the app
  cache, are never persisted, logged, or uploaded anywhere except the paired bridge.
- A file from the phone reaches the agent through the bridge: the phone sends `bridge_upload_file`,
  the bridge writes it under its state directory and returns the path, and the prompt carries that
  path. Never paste file contents into a prompt.
- Tokens live in the Android Keystore. They must not enter preferences, backups, logs or fixtures.
  Never print authorization headers or `.env` contents.
- One signing key signs every build, so updates install over each other. The key stays outside the
  repository, in the CI secrets and in the operator's Gradle home. Never commit a keystore.

## Shape of the code

- `app`: Compose UI, screen state, assistant surface, host and token storage.
- `core-rpc`: typed Pi commands and events, sanitized fixtures.
- `core-net`: WebSocket transport, auth, reconnect, request correlation.
- `core-sessions`: session index models and repositories.
- `bridge`: auth, envelopes, cwd and session locks, process lifecycle, extensions.

Keep tests beside the owning module. Protocol fixtures belong in `core-rpc/src/test/resources/rpc`
and must be sanitized. No dependency injection framework, database or navigation library until a
second caller justifies it.

## Code and comments

- Write the smallest change that solves the task, in the style of the file around it.
- Comment only the reason the code cannot show, in one line. No comment that restates the statement,
  no banners, no commented-out code, no emoji.
- A deliberate limit gets `shortcut: <the limit>, <when to upgrade>`.
- Never hardcode a colour, radius, type size or duration in a screen. Tokens live in
  `app/src/main/java/dev/scdeal/freskiel/ui/theme/` and nowhere else.
- Every screen shows its real states: empty, loading, error, busy. UI strings say what happened and
  what to do next, without exclamation marks.

## Build and checks

CI is the build machine. The development machine needs no Android SDK.

```sh
./gradlew clean ktlintCheck detekt test :benchmark:compileBenchmarkKotlin \
  :app:lintDebug :app:assembleDebug :app:assembleRelease
./gradlew :app:compileDebugAndroidTestKotlin
(cd bridge && pnpm install --frozen-lockfile && pnpm run check && pnpm audit --prod)
```

Toolchain: JDK 25 for Gradle and compilation (JDK 21 only for the detekt CLI), Android SDK 37,
Node 24+, pnpm 10, Pi 0.80.6+. Do not run connected or device tests unless the operator asks.

## Git

- `master` is always buildable. Work on a short-lived branch:
  `feat/<slug>`, `fix/<slug>`, `perf/<slug>`, `docs/<slug>`, `chore/<slug>`, `assistant/<slug>`.
- Commits follow Conventional Commits: `type(scope): imperative summary`, types `feat`, `fix`,
  `perf`, `refactor`, `test`, `docs`, `chore`, `build`, `ci`. One concern per commit, no `wip`.
- Open a pull request for every change, including solo work: CI is the gate. Squash merge, and keep
  the PR title in Conventional Commit form.
- Commit locally as work lands. Push in batches, only when asked. Never force-push `master` and
  never rewrite pushed history.
- `upstream` points at `ayagmar/pi-mobile`. Sync deliberately, not on every change; upstream code is
  a reference, not an authority over FRESKIEL's direction.
- Before committing: `git diff --check`, the focused tests for the touched module, and the UI opened
  in both themes when a screen changed.

## Definition of done

1. The behaviour works and its callers, fixtures and docs are updated.
2. A test covers new non-trivial logic; trivial changes need none.
3. `git diff --check` is clean and the touched checks pass in CI.
4. No secret, no generated build output and no local path is in the diff.
