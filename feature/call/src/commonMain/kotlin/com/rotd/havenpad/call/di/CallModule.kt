package com.rotd.havenpad.call.di

import com.rotd.havenpad.call.LiveTourCallViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val callModule = module {
    viewModel<LiveTourCallViewModel> { params ->
        LiveTourCallViewModel(sessionId = params.get())
    }
}
