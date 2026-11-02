package com.rotd.havenpad.rent.di

import com.rotd.havenpad.rent.RentViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val rentModule = module {
    viewModel<RentViewModel> { RentViewModel(rentRepository = get()) }
}
