package com.rotd.kejasafe.profile.di

import com.rotd.kejasafe.profile.EditProfileViewModel
import com.rotd.kejasafe.profile.ProfileViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val profileModule = module {
    viewModel<ProfileViewModel> {
        ProfileViewModel(
            profileRepository = get(),
            authRepository = get(),
        )
    }
    viewModel<EditProfileViewModel> { EditProfileViewModel(authRepository = get()) }
}
