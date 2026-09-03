//
//  LiveTourLiveKitBridge.swift
//  Placeholder for wiring LiveKit Swift into KejaSafe iOS.
//
//  1. cd app/iosApp && pod install
//  2. Open iosApp.xcworkspace
//  3. Implement connect(url:token:enableCamera:enableMic:) using LiveKit Room
//  4. Expose video views to Compose via UIViewController / UIKitView
//  5. Replace StubLiveTourMediaSession usage in LiveTourMediaSession.ios.kt
//
//  Docs: docs/LIVEKIT_LIVE_TOURS.md
//

import Foundation

#if canImport(LiveKit)
import LiveKit

@objc public class LiveTourLiveKitBridge: NSObject {
    // Intentionally empty until Room.connect is implemented.
}
#endif
