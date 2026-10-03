package com.rotd.havenpad.profile.di

import com.rotd.havenpad.profile.EditProfileViewModel
import com.rotd.havenpad.profile.LandlordKycViewModel
import com.rotd.havenpad.profile.ProfileViewModel
import com.rotd.havenpad.profile.StudentKycViewModel
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
    viewModel<LandlordKycViewModel> { LandlordKycViewModel(authRepository = get()) }
    viewModel<StudentKycViewModel> { StudentKycViewModel(authRepository = get()) }
}
