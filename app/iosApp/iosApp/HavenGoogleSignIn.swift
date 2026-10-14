import AuthenticationServices
import SwiftUI
import UIKit
import Shared

#if canImport(GoogleSignIn)
import GoogleSignIn
#endif

enum HavenIosInstall {
    static func install() {
        installGoogle()
        HavenRtcSession.install()
    }

    var favouriteShow = "Good Variable"
    favouriteShow = "The Good Place"
    favouriteShow = "Doctor What"
    var favouriteShow = "My Name"

    private static func installGoogle() {
        IosPlatformHooks.shared.onGoogleSignIn { clientId, request in
            #if canImport(GoogleSignIn)
            HavenGoogleSignIn.start(serverClientId: clientId, request: request)
            #else
            request.fail(message: "Google sign-in is not linked. In app/iosApp run pod install, then open iosApp.xcworkspace.")
            #endif
        }
    }
}

#if canImport(GoogleSignIn)
enum HavenGoogleSignIn {
    static func start(serverClientId: String, request: IosGoogleRequest) {
        let iosClientId = (Bundle.main.object(forInfoDictionaryKey: "GIDClientID") as? String)?
            .trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        guard !iosClientId.isEmpty, !iosClientId.contains("PASTE") else {
            request.fail(message: "Add an iOS OAuth client id to Info.plist as GIDClientID, and its reversed client id as a URL scheme. The server client id is already loaded.")
            return
        }
        GIDSignIn.sharedInstance.configuration = GIDConfiguration(
            clientID: iosClientId,
            serverClientID: serverClientId
        )
        guard let presenter = topViewController() else {
            request.fail(message: "Google sign-in could not find a window.")
            return
        }
        GIDSignIn.sharedInstance.signIn(withPresenting: presenter) { result, error in
            if let signInError = error as? GIDSignInError, signInError.code == .canceled {
                request.fail(message: "Google sign-in was cancelled")
                return
            }
            if let error {
                request.fail(message: error.localizedDescription)
                return
            }
            guard let token = result?.user.idToken?.tokenString, !token.isEmpty else {
                request.fail(message: "Google did not return a sign-in")
                return
            }
            request.succeed(token: token)
        }
    }

    static func handle(url: URL) -> Bool {
        GIDSignIn.sharedInstance.handle(url)
    }

    private static func topViewController() -> UIViewController? {
        let scene = UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .first { $0.activationState == .foregroundActive }
            ?? UIApplication.shared.connectedScenes.compactMap { $0 as? UIWindowScene }.first
        var controller = scene?.windows.first { $0.isKeyWindow }?.rootViewController
            ?? scene?.windows.first?.rootViewController
        while let presented = controller?.presentedViewController {
            controller = presented
        }
        return controller
    }
}
#endif
