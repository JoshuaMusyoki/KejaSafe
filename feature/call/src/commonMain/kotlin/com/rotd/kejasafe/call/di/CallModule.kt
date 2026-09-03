package com.rotd.kejasafe.call.di

import com.rotd.kejasafe.call.LiveTourCallViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val callModule = module {
    viewModel<LiveTourCallViewModel> { params ->
        LiveTourCallViewModel(sessionId = params.get())
    }
}
