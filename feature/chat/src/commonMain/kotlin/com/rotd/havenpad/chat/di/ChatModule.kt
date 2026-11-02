package com.rotd.havenpad.chat.di

import com.rotd.havenpad.chat.ChatInboxViewModel
import com.rotd.havenpad.chat.ChatThreadViewModel
import com.rotd.havenpad.domain.chat.ApartmentChatPeer
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val chatModule = module {
    viewModel { ChatInboxViewModel(chatRepository = get()) }
    viewModel { (peer: ApartmentChatPeer) ->
        ChatThreadViewModel(
            chatRepository = get(),
            peer = peer,
        )
    }
}
