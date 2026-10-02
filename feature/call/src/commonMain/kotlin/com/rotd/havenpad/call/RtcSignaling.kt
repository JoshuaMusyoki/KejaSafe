package com.rotd.havenpad.call

import com.rotd.havenpad.RtcIceCandidate
import com.rotd.havenpad.RtcRoomSnapshot
import com.rotd.havenpad.RtcSessionDescription
import com.rotd.havenpad.network.LiveTourClient
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Peer connection used by the signaling loop.
 * The loop follows the Firebase RTC codelab: offer, answer, then ICE candidates.
 */
interface RtcMediaEngine {
    suspend fun prepare(enableCamera: Boolean, enableMic: Boolean)
    suspend fun createOffer(): RtcSessionDescription
    suspend fun acceptRemoteOffer(offer: RtcSessionDescription): RtcSessionDescription
    suspend fun acceptRemoteAnswer(answer: RtcSessionDescription)
    suspend fun addIceCandidate(candidate: RtcIceCandidate)
    fun drainLocalIce(): List<RtcIceCandidate>
    fun setMicEnabled(enabled: Boolean)
    fun setCameraEnabled(enabled: Boolean)
    fun hasRemoteVideo(): Boolean
    fun close()
}

/** Browser bridge. Phone calls use [RtcMediaEngine] directly. */
interface RtcJsBridge {
    fun install()
    fun prepare(video: Boolean, audio: Boolean)
    fun phase(): String
    fun error(): String
    fun makeOffer()
    fun takeOffer(json: String)
    fun takeAnswer(json: String)
    fun addIce(json: String)
    fun drainIce(): String
    fun peekOffer(): String
    fun peekAnswer(): String
    fun setMic(on: Boolean)
    fun setCam(on: Boolean)
    fun hangup()
    fun hasRemote(): Boolean
}

internal const val HAVEN_RTC_INSTALL = """
(function () {
  if (globalThis.__haven) return;
  var state = {
    pc: null,
    local: null,
    localIce: [],
    applied: {},
    phase: "idle",
    offer: "",
    answer: "",
    error: "",
    remote: false
  };
  function ensureVideo(id, style) {
    var el = document.getElementById(id);
    if (!el) {
      el = document.createElement("video");
      el.id = id;
      el.autoplay = true;
      el.playsInline = true;
      el.setAttribute("playsinline", "true");
      el.style.cssText = style;
      document.body.appendChild(el);
    }
    return el;
  }
  globalThis.__haven = {
    state: state,
    prepare: function (video, audio) {
      state.phase = "starting";
      state.error = "";
      state.remote = false;
      var remoteEl = ensureVideo("haven-remote-video", "position:fixed;left:0;right:0;top:64px;bottom:210px;width:100%;height:auto;object-fit:cover;z-index:20;background:#111;pointer-events:none;");
      var localEl = ensureVideo("haven-local-video", "position:fixed;top:76px;right:12px;width:120px;height:160px;object-fit:cover;z-index:21;border-radius:12px;pointer-events:none;background:#222;");
      localEl.muted = true;
      navigator.mediaDevices.getUserMedia({ audio: !!audio, video: !!video }).then(function (stream) {
        state.local = stream;
        if (video) localEl.srcObject = stream;
        var pc = new RTCPeerConnection({
          iceServers: [{ urls: ["stun:stun1.l.google.com:19302", "stun:stun2.l.google.com:19302"] }]
        });
        state.pc = pc;
        stream.getTracks().forEach(function (track) { pc.addTrack(track, stream); });
        pc.onicecandidate = function (event) {
          if (event.candidate) state.localIce.push(event.candidate.toJSON());
        };
        pc.ontrack = function (event) {
          remoteEl.srcObject = event.streams[0];
          state.remote = true;
          state.phase = "remote";
        };
        state.phase = "ready";
      }).catch(function (err) {
        state.error = (err && err.message) ? err.message : String(err);
        state.phase = "error";
      });
    },
    makeOffer: function () {
      var pc = state.pc;
      if (!pc) return;
      pc.createOffer().then(function (offer) {
        return pc.setLocalDescription(offer);
      }).then(function () {
        state.offer = JSON.stringify(pc.localDescription);
        state.phase = "offered";
      }).catch(function (err) {
        state.error = (err && err.message) ? err.message : String(err);
        state.phase = "error";
      });
    },
    takeOffer: function (json) {
      var pc = state.pc;
      if (!pc) return;
      var desc = JSON.parse(json);
      pc.setRemoteDescription(desc).then(function () {
        return pc.createAnswer();
      }).then(function (answer) {
        return pc.setLocalDescription(answer);
      }).then(function () {
        state.answer = JSON.stringify(pc.localDescription);
        state.phase = "answered";
      }).catch(function (err) {
        state.error = (err && err.message) ? err.message : String(err);
        state.phase = "error";
      });
    },
    takeAnswer: function (json) {
      var pc = state.pc;
      if (!pc) return;
      pc.setRemoteDescription(JSON.parse(json)).catch(function (err) {
        state.error = (err && err.message) ? err.message : String(err);
        state.phase = "error";
      });
    },
    addIce: function (json) {
      var pc = state.pc;
      if (!pc) return;
      var candidate = JSON.parse(json);
      if (!candidate.candidate || state.applied[candidate.candidate]) return;
      state.applied[candidate.candidate] = true;
      pc.addIceCandidate(candidate).catch(function () {});
    },
    drainIce: function () {
      var batch = JSON.stringify(state.localIce);
      state.localIce = [];
      return batch;
    },
    setMic: function (on) {
      if (!state.local) return;
      state.local.getAudioTracks().forEach(function (track) { track.enabled = !!on; });
    },
    setCam: function (on) {
      if (!state.local) return;
      state.local.getVideoTracks().forEach(function (track) { track.enabled = !!on; });
    },
    hangup: function () {
      if (state.pc) state.pc.close();
      if (state.local) state.local.getTracks().forEach(function (track) { track.stop(); });
      ["haven-remote-video", "haven-local-video"].forEach(function (id) {
        var el = document.getElementById(id);
        if (el) el.remove();
      });
      state.pc = null;
      state.local = null;
      state.localIce = [];
      state.offer = "";
      state.answer = "";
      state.remote = false;
      state.phase = "idle";
    }
  };
})();
"""

private val rtcJson = Json { ignoreUnknownKeys = true }

class BrowserRtcEngine(
    private val bridge: RtcJsBridge,
) : RtcMediaEngine {
    override suspend fun prepare(enableCamera: Boolean, enableMic: Boolean) {
        bridge.install()
        bridge.prepare(enableCamera, enableMic)
        waitUntil { phase -> phase == "ready" || phase == "remote" }
    }

    override suspend fun createOffer(): RtcSessionDescription {
        bridge.makeOffer()
        val raw = waitText { bridge.peekOffer() }
        return rtcJson.decodeFromString(RtcSessionDescription.serializer(), raw)
    }

    override suspend fun acceptRemoteOffer(offer: RtcSessionDescription): RtcSessionDescription {
        bridge.takeOffer(rtcJson.encodeToString(RtcSessionDescription.serializer(), offer))
        val raw = waitText { bridge.peekAnswer() }
        return rtcJson.decodeFromString(RtcSessionDescription.serializer(), raw)
    }

    override suspend fun acceptRemoteAnswer(answer: RtcSessionDescription) {
        bridge.takeAnswer(rtcJson.encodeToString(RtcSessionDescription.serializer(), answer))
        delay(300)
    }

    override suspend fun addIceCandidate(candidate: RtcIceCandidate) {
        bridge.addIce(rtcJson.encodeToString(RtcIceCandidate.serializer(), candidate))
    }

    override fun drainLocalIce(): List<RtcIceCandidate> {
        val raw = bridge.drainIce()
        if (raw.isBlank() || raw == "[]") return emptyList()
        return runCatching {
            rtcJson.decodeFromString(ListSerializer(RtcIceCandidate.serializer()), raw)
        }.getOrDefault(emptyList())
    }

    override fun setMicEnabled(enabled: Boolean) = bridge.setMic(enabled)

    override fun setCameraEnabled(enabled: Boolean) = bridge.setCam(enabled)

    override fun hasRemoteVideo(): Boolean = bridge.hasRemote()

    override fun close() = bridge.hangup()

    private suspend fun waitUntil(ready: (String) -> Boolean) {
        var waited = 0
        while (waited < 20_000) {
            val phase = bridge.phase()
            if (phase == "error") {
                throw IllegalStateException(bridge.error().ifBlank { "Could not start the call" })
            }
            if (ready(phase)) return
            delay(200)
            waited += 200
        }
        throw IllegalStateException(bridge.error().ifBlank { "The call took too long to start" })
    }

    private suspend fun waitText(read: () -> String): String {
        var waited = 0
        while (waited < 20_000) {
            if (bridge.phase() == "error") {
                throw IllegalStateException(bridge.error().ifBlank { "Call setup failed" })
            }
            val value = read()
            if (value.isNotBlank()) return value
            delay(200)
            waited += 200
        }
        throw IllegalStateException("The other person did not finish connecting")
    }
}

suspend fun runWebRtcSignaling(
    sessionId: String,
    asCaller: Boolean,
    enableCamera: Boolean,
    enableMic: Boolean,
    engine: RtcMediaEngine,
    onState: (LiveTourConnectionState, String?) -> Unit,
    onRemoteVideo: (Boolean) -> Unit,
) {
    onState(LiveTourConnectionState.Connecting, "Starting camera and microphone…")
    engine.prepare(enableCamera, enableMic)
    val seen = mutableSetOf<String>()
    suspend fun flushLocalIce() {
        engine.drainLocalIce().forEach { candidate ->
            LiveTourClient.postIce(sessionId, candidate).getOrThrow()
        }
    }
    suspend fun pullRemote(snapshot: RtcRoomSnapshot) {
        val remote = if (asCaller) snapshot.calleeCandidates else snapshot.callerCandidates
        remote.forEach { candidate ->
            if (seen.add(candidate.candidate)) engine.addIceCandidate(candidate)
        }
        onRemoteVideo(engine.hasRemoteVideo())
    }

    if (asCaller) {
        onState(LiveTourConnectionState.Connecting, "Calling…")
        val offer = engine.createOffer()
        LiveTourClient.postOffer(sessionId, offer).getOrThrow()
        var answerApplied = false
        while (coroutineContext.isActive) {
            flushLocalIce()
            val snapshot = LiveTourClient.rtcRoom(sessionId).getOrThrow()
            val answer = snapshot.answer
            if (!answerApplied && answer != null) {
                engine.acceptRemoteAnswer(answer)
                answerApplied = true
                onState(LiveTourConnectionState.Connected, "Connected")
            } else if (!answerApplied) {
                onState(LiveTourConnectionState.Connecting, "Waiting for the other person to join…")
            }
            pullRemote(snapshot)
            delay(700)
        }
    } else {
        onState(LiveTourConnectionState.Connecting, "Waiting for the landlord to start the call…")
        var offer: RtcSessionDescription? = null
        while (offer == null && coroutineContext.isActive) {
            val snapshot = LiveTourClient.rtcRoom(sessionId).getOrThrow()
            offer = snapshot.offer
            if (offer == null) delay(700)
        }
        if (!coroutineContext.isActive || offer == null) return
        onState(LiveTourConnectionState.Connecting, "Joining…")
        val answer = engine.acceptRemoteOffer(offer)
        LiveTourClient.postAnswer(sessionId, answer).getOrThrow()
        onState(LiveTourConnectionState.Connected, "Connected")
        while (coroutineContext.isActive) {
            flushLocalIce()
            val snapshot = LiveTourClient.rtcRoom(sessionId).getOrThrow()
            pullRemote(snapshot)
            delay(700)
        }
    }
}
