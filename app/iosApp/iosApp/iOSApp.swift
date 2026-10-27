import SwiftUI

@main
struct iOSApp: App {
    var body: some Scene {
        WindowGroup {
            ContentView()
                .onOpenURL { url in
                    #if canImport(GoogleSignIn)
                    _ = HavenGoogleSignIn.handle(url: url)
                    #endif
                }
        }
    }
}