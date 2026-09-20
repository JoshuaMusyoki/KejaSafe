package com.rotd.kejasafe.listing.di

import com.rotd.kejasafe.listing.CreateListingViewModel
import com.rotd.kejasafe.listing.LandlordHomeViewModel
import com.rotd.kejasafe.listing.ListingAnalyticsViewModel
import com.rotd.kejasafe.listing.ListingCalendarViewModel
import com.rotd.kejasafe.listing.RequestPropertyViewModel
import com.rotd.kejasafe.listing.TenantHomeViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val listingModule = module {
    viewModel<LandlordHomeViewModel> {
        LandlordHomeViewModel(listingRepository = get(), escrowRepository = get()) }
    viewModel<TenantHomeViewModel> { TenantHomeViewModel(listingRepository = get()) }
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
