package com.rotd.havenpad.listing.di

import com.rotd.havenpad.listing.CreateListingViewModel
import com.rotd.havenpad.listing.viewModel.LandlordHomeViewModel
import com.rotd.havenpad.listing.viewModel.ListingAnalyticsViewModel
import com.rotd.havenpad.listing.viewModel.ListingCalendarViewModel
import com.rotd.havenpad.listing.viewModel.ListingSearchViewModel
import com.rotd.havenpad.listing.viewModel.RequestPropertyViewModel
import com.rotd.havenpad.listing.viewModel.TenantHomeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val listingModule = module {
    viewModel<LandlordHomeViewModel> {
        LandlordHomeViewModel(listingRepository = get(), escrowRepository = get()) }
    viewModel<TenantHomeViewModel> { TenantHomeViewModel(listingRepository = get()) }
    viewModel<ListingSearchViewModel> { ListingSearchViewModel(listingRepository = get()) }
    viewModel<RequestPropertyViewModel> { RequestPropertyViewModel(repository = get()) }
    viewModel<CreateListingViewModel> { params ->
        CreateListingViewModel(
            editingListingId = params.getOrNull<String>(),
            listingRepository = get()
        )
    }
    viewModel<ListingAnalyticsViewModel> { params ->
        ListingAnalyticsViewModel(listingId = params.get<String>())
    }
    viewModel<ListingCalendarViewModel> { params ->
        ListingCalendarViewModel(
            listingId = params.get<String>(),
            escrowRepository = get()
        )
    }
}
