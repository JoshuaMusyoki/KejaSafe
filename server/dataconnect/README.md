# Firebase Data Connect (Cloud SQL) — paused

Lives under **`:server`** so the future Postgres/Data Connect schema sits with the backend.
**Right now the running stack is H2 + Ktor** (`server` module). Cloud SQL is not required.

## Current vs later

| Now (active) | Later (this folder) |
|---|---|
| `:server` + H2 file DB | Firebase Data Connect → Cloud SQL `kejasafe-fdc` |
| Android profiles: local `SessionCachedUserProfileDataSource` | `SqlConnectUserProfileDataSource` + generated SDK |
| Listings/rent: `ListingClient` → Ktor | `KejasafeConnector` mutations/queries |

## Layout

- `schema/schema.gql` — Postgres model (Firebase Auth uid = `User.id`)
- `kejasafe/` — connector (queries + mutations + Kotlin SDK config)
- `dataconnect.yaml` — service / **Cloud SQL** binding (`kejasafe-fdc`)

## Re-enable Cloud SQL later

```bash
# from repo root
firebase login
firebase use kejasafe
firebase dataconnect:sql:setup    # creates/links Cloud SQL (billing required)
firebase deploy --only dataconnect
firebase dataconnect:sdk:generate
```

Then in the Android app:

1. `createUserProfileDataSource()` → `SqlConnectUserProfileDataSource()`
2. Uncomment Cloud SQL blocks in `SqlConnectUserProfileDataSource`
3. `DataConnectRuntime.cloudEnabled = true` in `HavenPadApplication`

## Auth note

Passwords are **not** stored in SQL. Use Firebase Auth; upsert the `User` row with `UpsertMyUser` after sign-in/register.
