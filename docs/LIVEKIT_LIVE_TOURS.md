# LiveKit live house tours

Set these environment variables before starting `:server`:

```bash
# Windows PowerShell
$env:LIVEKIT_URL="wss://YOUR_PROJECT.livekit.cloud"
$env:LIVEKIT_API_KEY="APIxxxxxxxx"
$env:LIVEKIT_API_SECRET="secretxxxxxxxx"

# then
./gradlew :server:run
```

Create a project at https://cloud.livekit.io and copy the URL + API key/secret.

## Flow

1. Tenant opens a listing → **Request live tour** (preferred time + video/audio)
2. Landlord Bookings → **Confirm** (requires LiveKit env) → opens call screen → **Join**
3. Tenant sees **Join live tour** on the listing when status is READY/ACTIVE
4. Both connect with short-lived tokens; Compose overlay applies a moving watermark

## Platforms

- **Android:** LiveKit Android SDK (`io.livekit:livekit-android`) + runtime CAMERA/RECORD_AUDIO prompts
- **iOS:** After `cd app/iosApp && pod install`, open `iosApp.xcworkspace`. Live tours use the same WebRTC offer/answer path as Android and the website (`LiveKitWebRTC`). Google sign-in needs an iOS OAuth client id in `Info.plist` (`GIDClientID`) plus the reversed client id URL scheme. Alerts while the app is open are local notifications; killed-app push is not set up.

## Watermark

Deterrent overlay (listing · viewer · id · LIVE timer). Does **not** block OS screen capture.

## API cheat sheet

| Method | Path |
|--------|------|
| POST | `/listings/{id}/live-tours` |
| POST | `/live-tours/{id}/confirm` |
| POST | `/live-tours/{id}/reschedule` |
| POST | `/live-tours/{id}/token` |
| POST | `/live-tours/{id}/end` |
| GET | `/live-tours/{id}` |
| GET | `/live-tours/mine` |
