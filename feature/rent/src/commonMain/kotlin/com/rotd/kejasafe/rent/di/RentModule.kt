package com.rotd.kejasafe.rent.di

import com.rotd.kejasafe.rent.RentViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val rentModule = module {
    viewModel<RentViewModel> { RentViewModel(rentRepository = get()) }
}
