package com.rotd.kejasafe.chat.di

import com.rotd.kejasafe.chat.ChatInboxViewModel
import com.rotd.kejasafe.chat.ChatThreadViewModel
import com.rotd.kejasafe.domain.chat.ApartmentChatPeer
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
