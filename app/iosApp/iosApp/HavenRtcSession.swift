import AVFoundation
import UIKit
import Shared

#if canImport(LiveKitWebRTC)
import LiveKitWebRTC
#endif

enum HavenRtcSession {
    static func install() {
        #if canImport(LiveKitWebRTC)
        let engine = HavenWebRtcEngine.shared
        IosPlatformHooks.shared.onPrepareCall { camera, mic in
            engine.prepare(camera: camera, mic: mic)
        }
        IosPlatformHooks.shared.onCreateOffer {
            engine.createOffer()
        }
        IosPlatformHooks.shared.onAcceptOffer { type, sdp in
            engine.acceptOffer(type: type, sdp: sdp)
        }
        IosPlatformHooks.shared.onAcceptAnswer { type, sdp in
            engine.acceptAnswer(type: type, sdp: sdp)
        }
        IosPlatformHooks.shared.onAddIce { candidate, sdpMid, line in
            engine.addIce(candidate: candidate, sdpMid: sdpMid, line: line)
        }
        IosPlatformHooks.shared.onSetMic { enabled in
            engine.setMic(enabled)
        }
        IosPlatformHooks.shared.onSetCamera { enabled in
            engine.setCamera(enabled)
        }
        IosPlatformHooks.shared.onHangUp {
            engine.hangUp()
        }
        IosPlatformHooks.shared.onAttachVideo { view, remote in
            engine.attach(view: view, remote: remote)
        }
        IosPlatformHooks.shared.onDetachVideo { view, remote in
            engine.detach(view: view, remote: remote)
        }
        #else
        IosPlatformHooks.shared.onPrepareCall { _, _ in
            IosPlatformHooks.shared.rtcFailed(message: "Live tour video is not linked. In app/iosApp run pod install, then open iosApp.xcworkspace.")
        }
        #endif
    }
}

#if canImport(LiveKitWebRTC)
final class HavenWebRtcEngine: NSObject, RTCPeerConnectionDelegate {
    static let shared = HavenWebRtcEngine()

    private let factory: RTCPeerConnectionFactory
    private var peer: RTCPeerConnection?
    private var audioTrack: RTCAudioTrack?
    private var videoTrack: RTCVideoTrack?
    private var remoteTrack: RTCVideoTrack?
    private var capturer: RTCCameraVideoCapturer?
    private var localRenderer: RTCMTLVideoView?
    private var remoteRenderer: RTCMTLVideoView?
    private var cameraOn = true

    private override init() {
        RTCInitializeSSL()
        factory = RTCPeerConnectionFactory()
        super.init()
    }

    func prepare(camera: Bool, mic: Bool) {
        DispatchQueue.main.async {
            self.hangUp()
            self.cameraOn = camera
            AVCaptureDevice.requestAccess(for: .audio) { audioGranted in
                AVCaptureDevice.requestAccess(for: .video) { videoGranted in
                    DispatchQueue.main.async {
                        self.start(
                            camera: camera && videoGranted,
                            mic: mic && audioGranted,
                            micDenied: mic && !audioGranted
                        )
                    }
                }
            }
        }
    }

    private func start(camera: Bool, mic: Bool, micDenied: Bool) {
        if micDenied {
            IosPlatformHooks.shared.rtcFailed(message: "Allow microphone access in Settings to join the tour.")
            return
        }
        let config = RTCConfiguration()
        config.sdpSemantics = .unifiedPlan
        config.iceServers = [
            RTCIceServer(urlStrings: ["stun:stun.l.google.com:19302"]),
            RTCIceServer(urlStrings: ["stun:stun1.l.google.com:19302"]),
        ]
        let constraints = RTCMediaConstraints(mandatoryConstraints: nil, optionalConstraints: nil)
        guard let connection = factory.peerConnection(with: config, constraints: constraints, delegate: self) else {
            IosPlatformHooks.shared.rtcFailed(message: "Could not start WebRTC")
            return
        }
        peer = connection
        let audio = factory.audioTrack(with: factory.audioSource(with: nil), trackId: "haven-audio")
        audio.isEnabled = mic
        connection.add(audio, streamIds: ["haven"])
        audioTrack = audio
        if camera {
            startCamera(on: connection) {
                IosPlatformHooks.shared.rtcReady()
            }
        } else {
            IosPlatformHooks.shared.rtcReady()
        }
    }

    func createOffer() {
        guard let connection = peer else {
            IosPlatformHooks.shared.rtcFailed(message: "Call is not ready")
            return
        }
        let constraints = mediaConstraints()
        connection.offer(for: constraints) { sdp, error in
            if let error {
                IosPlatformHooks.shared.rtcFailed(message: error.localizedDescription)
                return
            }
            guard let sdp else {
                IosPlatformHooks.shared.rtcFailed(message: "Could not create the call offer")
                return
            }
            connection.setLocalDescription(sdp) { error in
                if let error {
                    IosPlatformHooks.shared.rtcFailed(message: error.localizedDescription)
                    return
                }
                IosPlatformHooks.shared.rtcOffer(type: Self.name(for: sdp.type), sdp: sdp.sdp)
            }
        }
    }

    func acceptOffer(type: String, sdp: String) {
        guard let connection = peer else {
            IosPlatformHooks.shared.rtcFailed(message: "Call is not ready")
            return
        }
        let remote = RTCSessionDescription(type: Self.sdpType(type), sdp: sdp)
        connection.setRemoteDescription(remote) { error in
            if let error {
                IosPlatformHooks.shared.rtcFailed(message: error.localizedDescription)
                return
            }
            connection.answer(for: self.mediaConstraints()) { answer, error in
                if let error {
                    IosPlatformHooks.shared.rtcFailed(message: error.localizedDescription)
                    return
                }
                guard let answer else {
                    IosPlatformHooks.shared.rtcFailed(message: "Could not answer the call")
                    return
                }
                connection.setLocalDescription(answer) { error in
                    if let error {
                        IosPlatformHooks.shared.rtcFailed(message: error.localizedDescription)
                        return
                    }
                    IosPlatformHooks.shared.rtcAnswer(type: Self.name(for: answer.type), sdp: answer.sdp)
                }
            }
        }
    }

    func acceptAnswer(type: String, sdp: String) {
        guard let connection = peer else {
            IosPlatformHooks.shared.rtcFailed(message: "Call is not ready")
            return
        }
        let remote = RTCSessionDescription(type: Self.sdpType(type), sdp: sdp)
        connection.setRemoteDescription(remote) { error in
            if let error {
                IosPlatformHooks.shared.rtcFailed(message: error.localizedDescription)
            }
        }
    }

    func addIce(candidate: String, sdpMid: String, line: Int32) {
        let ice = RTCIceCandidate(sdp: candidate, sdpMLineIndex: line, sdpMid: sdpMid.isEmpty ? nil : sdpMid)
        peer?.add(ice)
    }

    func setMic(_ enabled: Bool) {
        audioTrack?.isEnabled = enabled
    }

    func setCamera(_ enabled: Bool) {
        cameraOn = enabled
        videoTrack?.isEnabled = true
        if enabled {
            guard let connection = peer else { return }
            if capturer == nil {
                startCamera(on: connection, done: {})
            } else if let device = frontCamera(), let format = preferredFormat(for: device) {
                capturer?.startCapture(with: device, format: format, fps: preferredFps(for: format))
            }
        } else {
            capturer?.stopCapture()
        }
    }

    func hangUp() {
        capturer?.stopCapture()
        capturer = nil
        peer?.close()
        peer = nil
        audioTrack = nil
        videoTrack = nil
        remoteTrack = nil
        localRenderer?.removeFromSuperview()
        remoteRenderer?.removeFromSuperview()
        IosPlatformHooks.shared.rtcRemoteVideo(visible: false)
    }

    func attach(view: UIView, remote: Bool) {
        DispatchQueue.main.async {
            let renderer = self.renderer(remote: remote, fitting: view.bounds)
            renderer.frame = view.bounds
            renderer.autoresizingMask = [.flexibleWidth, .flexibleHeight]
            if renderer.superview !== view {
                renderer.removeFromSuperview()
                view.addSubview(renderer)
            }
            if remote {
                self.remoteTrack?.add(renderer)
            } else {
                self.videoTrack?.add(renderer)
            }
        }
    }

    func detach(view: UIView, remote: Bool) {
        DispatchQueue.main.async {
            let renderer = remote ? self.remoteRenderer : self.localRenderer
            if renderer?.superview === view {
                if remote {
                    if let renderer { self.remoteTrack?.remove(renderer) }
                } else if let renderer {
                    self.videoTrack?.remove(renderer)
                }
                renderer?.removeFromSuperview()
            }
        }
    }

    private func renderer(remote: Bool, fitting bounds: CGRect) -> RTCMTLVideoView {
        if remote {
            if let remoteRenderer { return remoteRenderer }
            let created = RTCMTLVideoView(frame: bounds)
            created.videoContentMode = .scaleAspectFill
            remoteRenderer = created
            return created
        }
        if let localRenderer { return localRenderer }
        let created = RTCMTLVideoView(frame: bounds)
        created.videoContentMode = .scaleAspectFill
        localRenderer = created
        return created
    }

    private func startCamera(on connection: RTCPeerConnection, done: @escaping () -> Void) {
        guard let device = frontCamera(), let format = preferredFormat(for: device) else {
            done()
            return
        }
        let source = factory.videoSource()
        let track = factory.videoTrack(with: source, trackId: "haven-video")
        track.isEnabled = true
        connection.add(track, streamIds: ["haven"])
        videoTrack = track
        localRenderer.map { track.add($0) }
        let camera = RTCCameraVideoCapturer(delegate: source)
        capturer = camera
        camera.startCapture(with: device, format: format, fps: preferredFps(for: format)) { _ in
            done()
        }
    }

    private func frontCamera() -> AVCaptureDevice? {
        let devices = RTCCameraVideoCapturer.captureDevices()
        return devices.first { $0.position == .front } ?? devices.first
    }

    private func preferredFormat(for device: AVCaptureDevice) -> AVCaptureDevice.Format? {
        RTCCameraVideoCapturer.supportedFormats(for: device).last
    }

    private func preferredFps(for format: AVCaptureDevice.Format) -> Int {
        let maxFps = format.videoSupportedFrameRateRanges.map(\.maxFrameRate).max() ?? 30
        return Int(min(maxFps, 30))
    }

    private func mediaConstraints() -> RTCMediaConstraints {
        RTCMediaConstraints(
            mandatoryConstraints: [
                kRTCMediaConstraintsOfferToReceiveAudio: kRTCMediaConstraintsValueTrue,
                kRTCMediaConstraintsOfferToReceiveVideo: kRTCMediaConstraintsValueTrue,
            ],
            optionalConstraints: nil
        )
    }

    private static func name(for type: RTCSdpType) -> String {
        switch type {
        case .answer: return "answer"
        default: return "offer"
        }
    }

    private static func sdpType(_ name: String) -> RTCSdpType {
        name == "answer" ? .answer : .offer
    }

    func peerConnection(_ peerConnection: RTCPeerConnection, didChange stateChanged: RTCSignalingState) {}

    func peerConnection(_ peerConnection: RTCPeerConnection, didAdd stream: RTCMediaStream) {
        if let track = stream.videoTracks.first {
            adoptRemote(track)
        }
    }

    func peerConnection(_ peerConnection: RTCPeerConnection, didRemove stream: RTCMediaStream) {}

    func peerConnectionShouldNegotiate(_ peerConnection: RTCPeerConnection) {}

    func peerConnection(_ peerConnection: RTCPeerConnection, didChange newState: RTCIceConnectionState) {}

    func peerConnection(_ peerConnection: RTCPeerConnection, didChange newState: RTCIceGatheringState) {}

    func peerConnection(_ peerConnection: RTCPeerConnection, didGenerate candidate: RTCIceCandidate) {
        IosPlatformHooks.shared.rtcIce(
            candidate: candidate.sdp,
            sdpMid: candidate.sdpMid ?? "",
            sdpMLineIndex: Int32(candidate.sdpMLineIndex)
        )
    }

    func peerConnection(_ peerConnection: RTCPeerConnection, didRemove candidates: [RTCIceCandidate]) {}

    func peerConnection(_ peerConnection: RTCPeerConnection, didOpen dataChannel: RTCDataChannel) {}

    func peerConnection(
        _ peerConnection: RTCPeerConnection,
        didAdd rtpReceiver: RTCRtpReceiver,
        streams: [RTCMediaStream]
    ) {
        if let track = rtpReceiver.track as? RTCVideoTrack {
            adoptRemote(track)
        }
    }

    private func adoptRemote(_ track: RTCVideoTrack) {
        remoteTrack = track
        if let remoteRenderer {
            track.add(remoteRenderer)
        }
        IosPlatformHooks.shared.rtcRemoteVideo(visible: true)
    }
}
#endif
