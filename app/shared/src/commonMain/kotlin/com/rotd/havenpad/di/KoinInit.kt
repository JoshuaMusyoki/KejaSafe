package com.rotd.havenpad.di

import com.rotd.havenpad.auth.LoginViewModel
import com.rotd.havenpad.auth.RegisterViewModel
import com.rotd.havenpad.call.di.callModule
import com.rotd.havenpad.chat.di.chatModule
import com.rotd.havenpad.listing.di.listingModule
import com.rotd.havenpad.profile.di.profileModule
import com.rotd.havenpad.rent.di.rentModule
import com.rotd.havenpad.web.LandingViewModel
import org.koin.core.context.startKoin
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module
import org.koin.mp.KoinPlatform

val authModule = module {
    viewModel<LoginViewModel> { LoginViewModel(authRepository = get()) }
    viewModel<RegisterViewModel> { RegisterViewModel(authRepository = get()) }
}

val webModule = module {
    viewModel<LandingViewModel> {
        LandingViewModel(listingRepository = get(), communityRepository = get())
    }
}

fun appModules() = listOf(
    coreModule,
    authModule,
    webModule,
    listingModule,
    rentModule,
    profileModule,
    chatModule,
    callModule,
)

/**
 * Start Koin once for the process. Safe to call from Android Application and iOS entry.
 */
fun initKoin(config: KoinAppDeclaration = {}) {
    if (KoinPlatform.getKoinOrNull() != null) return
    startKoin {
        config()
        modules(appModules())
    }
}
