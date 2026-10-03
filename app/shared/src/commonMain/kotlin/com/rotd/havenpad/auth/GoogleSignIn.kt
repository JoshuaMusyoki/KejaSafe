package com.rotd.havenpad.auth

import androidx.compose.runtime.Composable

/**
 * Opens the platform Google sign-in flow and returns an ID token.
 * [clientId] is the web client id published by the API.
 */
@Composable
expect fun rememberGoogleIdTokenLauncher(
    onToken: (String) -> Unit,
    onError: (String) -> Unit,
): (String) -> Unit

internal const val HAVEN_GOOGLE_SIGN_IN_SCRIPT = """
(function () {
  if (globalThis.__havenGoogleInstalled) return;
  globalThis.__havenGoogleInstalled = true;
  globalThis.__havenGoogleStart = function (clientId) {
    function finish(token, error) {
      var overlay = document.getElementById("haven-google-overlay");
      if (overlay) overlay.remove();
      var done = globalThis.__havenGoogleDone;
      if (done) done((token || "") + "\n" + (error || ""));
    }
    function openPicker() {
      var google = globalThis.google;
      if (!google || !google.accounts || !google.accounts.id) {
        finish("", "Google sign-in failed to load");
        return;
      }
      var old = document.getElementById("haven-google-overlay");
      if (old) old.remove();
      var overlay = document.createElement("div");
      overlay.id = "haven-google-overlay";
      overlay.style.cssText = "position:fixed;inset:0;z-index:2147483647;background:rgba(15,23,42,0.55);display:flex;align-items:center;justify-content:center;padding:24px;";
      var card = document.createElement("div");
      card.style.cssText = "background:#ffffff;color:#111827;padding:24px;border-radius:16px;display:flex;flex-direction:column;align-items:center;gap:16px;max-width:360px;width:100%;box-shadow:0 16px 40px rgba(0,0,0,0.2);";
      var title = document.createElement("div");
      title.textContent = "Continue with Google";
      title.style.cssText = "font:600 18px system-ui,sans-serif;";
      var host = document.createElement("div");
      var cancel = document.createElement("button");
      cancel.type = "button";
      cancel.textContent = "Cancel";
      cancel.style.cssText = "border:0;background:transparent;color:#4b5563;cursor:pointer;font:500 14px system-ui,sans-serif;";
      cancel.onclick = function () { finish("", "Google sign-in was cancelled"); };
      card.appendChild(title);
      card.appendChild(host);
      card.appendChild(cancel);
      overlay.appendChild(card);
      overlay.addEventListener("click", function (event) {
        if (event.target === overlay) finish("", "Google sign-in was cancelled");
      });
      document.body.appendChild(overlay);
      google.accounts.id.initialize({
        client_id: clientId,
        callback: function (resp) {
          if (resp && resp.credential) finish(resp.credential, "");
          else finish("", "Google did not return a sign-in");
        },
        auto_select: false,
        cancel_on_tap_outside: false
      });
      google.accounts.id.renderButton(host, {
        theme: "outline",
        size: "large",
        text: "signin_with",
        width: 280
      });
    }
    if (globalThis.google && globalThis.google.accounts && globalThis.google.accounts.id) {
      openPicker();
      return;
    }
    var script = document.querySelector("script[data-haven-gis]");
    if (!script) {
      script = document.createElement("script");
      script.src = "https://accounts.google.com/gsi/client";
      script.async = true;
      script.defer = true;
      script.setAttribute("data-haven-gis", "1");
      document.head.appendChild(script);
    }
    if (script.getAttribute("data-haven-loaded") === "1") {
      openPicker();
      return;
    }
    script.addEventListener("load", function () {
      script.setAttribute("data-haven-loaded", "1");
      openPicker();
    });
    script.addEventListener("error", function () {
      finish("", "Could not load Google sign-in");
    });
  };
})();
"""
