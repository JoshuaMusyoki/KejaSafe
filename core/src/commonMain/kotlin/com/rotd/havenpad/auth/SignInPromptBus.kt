package com.rotd.havenpad.auth

import com.rotd.havenpad.SignInPrompt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object SignInPromptBus {
    private val _prompt = MutableStateFlow<SignInPrompt?>(null)
    val prompt = _prompt.asStateFlow()

    fun offer(prompt: SignInPrompt) {
        if (prompt.challengeId.isBlank()) return
        _prompt.value = prompt
    }

    fun clear(challengeId: String) {
        if (_prompt.value?.challengeId == challengeId) _prompt.value = null
    }
}

class SignInAwaitingApproval(
    val challengeId: String,
    val deviceName: String,
) : Exception("Waiting for your other device to allow this sign-in.")
