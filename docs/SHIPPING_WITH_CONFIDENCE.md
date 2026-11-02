# HavenPad — shipping KMP with confidence

Companion notes for the Droidcon Uganda 2026 talk. The repository on GitHub is still named [KejaSafe](https://github.com/JoshuaMusyoki/KejaSafe). The product is HavenPad.

## The business

Students looking for a room near campus are asked to pay a deposit before they have seen the house, the landlord, or anyone who has lived there. HavenPad is a verified student-housing marketplace for that moment.

A student can browse houses without an account. A landlord publishes a house. A student reviews a house only after a confirmed stay, once, and that score belongs to that house. The landlord can reply. If a review is wrong, either person reports it and an admin follows up with the author, then deletes it if it does not belong on the platform. Landlords review students who stayed, on the same one-review rule.

The product promise is small and checkable:

- A house with no reviews says so. It does not borrow another house's percent.
- A neighborhood name appears only when the GPS fix is within 200 metres. Otherwise the heading is the city, or "Houses near you".
- Chat, bills, bookings, and profile are signed-in actions. Guest browsing stays on the house list.
- Killing the app or reloading the web tab asks for the password again. Switching away does not.

Android and the web app are the shipping targets. iOS is in the Kotlin source set and is not part of the pull-request gate.

## The modules

```
:core                 models, house ranking, place-name rule, chat timeline
:feature:listing      browse, search, listing editor, review panel
:feature:chat         inbox, thread, desktop sidebar
:feature:call         live tour call UI
:feature:rent         bills and due dates
:feature:profile      profile and the reputation line
:app:shared           navigation, landing, auth, admin
:app:androidApp       Android shell
:app:webApp           Wasm and JS shell
:server               Ktor API, reviews, bookings, health check
:build-logic          convention plugins that give every module the same targets
```

Feature modules depend on `:core`. The server depends on `:core`. Screens do not calculate rank or decide whether a GPS fix is tight enough to name a neighborhood. Those two rules live in `core/src/commonMain/kotlin/com/rotd/havenpad/trust/`.

`:build-logic` is what keeps the modules boring. `KotlinMultiplatformLibraryConventionPlugin` adds Android, iOS, JVM, JS, and Wasm, and it puts `kotlin-test` on `commonTest`. A new feature module does not copy that block.

## What each module proves

| Module | Test | Rule |
| --- | --- | --- |
| `:core` | `HouseRankingTest` | A 4-star review on Joymat is 80%, rank 1. Jaymoh, with no reviews, has no score. |
| `:core` | `BrowsePlaceTest` | 40 m may say "Kilimani, Nairobi". 800 m says "Nairobi" only. |
| `:core` | `ChatTimelineTest`, `ApartmentChatAttachmentTest` | Day headers and PDF previews. |
| `:feature:listing` | `BrowseHeadingTest` | Locating, named, coordinates-only, and empty each get their own heading. |
| `:feature:chat` | `ReceiptDescriptionTest` | Sending, sent, delivered, read, failed. |
| `:feature:rent` | `FormatDueTest` | A due date is a UTC calendar day, not a locale formatter. |
| `:feature:call` | `FormatElapsedTest` | Call timers print `hh:mm:ss`. |
| `:feature:profile` | `ReputationHeadlineTest` | Loading, no reviews, then percent and rank. |
| `:app:shared` | `SharedCommonTest` | Landing money format, and only students and landlords post a story. |
| `:server` | `ApplicationTest` | `GET /health` returns ok, and local defaults stay on H2 port 8080. |
| `:server` | `ReportQueueTest` | A blank report is not an admin queue item. A written one stays until it is dismissed. |

`commonTest` is compiled once and executed with `:module:jvmTest`. The same source is what Android and Wasm compile against. The JVM run is the fast proof.

Run them the way CI does:

```bash
./gradlew :core:jvmTest :feature:listing:jvmTest :feature:chat:jvmTest :feature:rent:jvmTest :feature:profile:jvmTest :feature:call:jvmTest :app:shared:jvmTest :server:test
```

## The pipeline

Three workflows. Pull requests only run the first.

**`.github/workflows/ci.yml`** on every pull request and on `master_dev` / `main`.

1. **Tests** (Ubuntu). The module list above. If this fails, nothing else starts.
2. **Android app**, after tests. `assembleDebug`, uploaded as an artifact. There is no release signing config in the repo.
3. **Server jar**, after tests. `buildFatJar`, then `docker build` of `server/Dockerfile`.
4. **Web compile**, after tests. `:app:webApp:compileKotlinWasmJs`.

Linking the Wasm browser binary takes about twenty-five minutes locally, so that step is not on the pull-request path. iOS tests need a Mac and are not on this path either. Both are still source-compatible because the convention plugin registers the targets.

**`.github/workflows/deploy-api.yml`** pushes the server image to Cloud Run when server, core, or build-logic changes land on `master_dev` or `main`. It does nothing until the `GCP_WORKLOAD_IDENTITY_PROVIDER` repository variable is set, so a fork push does not fail for a missing cloud project.

**`.github/workflows/deploy-web.yml`** publishes the web bundle to Firebase Hosting. Pull requests can get a preview URL. It needs `FIREBASE_SERVICE_ACCOUNT` and `KEJASAFE_API_BASE_URL`.

Cloud SQL, the uploads bucket, and the first Cloud Run service are created from a workstation with `deploy/deploy-api.ps1`. The GitHub workflow only replaces the image after that. Details are in [DEPLOYMENT.md](DEPLOYMENT.md).

## Local stack

JDK 21. The API owns port 8080 and the H2 file `server/build/db`. Only one process may open that file.

```bash
./gradlew :server:run
./gradlew :app:webApp:wasmJsBrowserDevelopmentRun --continuous
./gradlew :app:androidApp:installDebug
```

Web is http://localhost:3000. The page talks to port 8080 on the same host. A phone on the same Wi-Fi needs `ApiConfig.DEV_API_BASE_URL` pointed at the machine's LAN address.

## What this does not claim

- iOS push and an iOS CI job are not shipping.
- Google sign-in still needs `HAVENPAD_GOOGLE_CLIENT_ID`.
- Killed-app alerts need a Firebase service-account file on the server.
- The admin does not auto-verify KYC.
- Deploy workflows stay idle until the cloud variables exist. CI does not.
