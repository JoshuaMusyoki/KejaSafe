package com.rotd.kejasafe.di

import com.rotd.kejasafe.data.ListingLocalStore
import com.rotd.kejasafe.data.ListingRepository
import com.rotd.kejasafe.data.ProfileRepository
import com.rotd.kejasafe.data.RentRepository
import com.rotd.kejasafe.data.UserProfileDataSource
import com.rotd.kejasafe.data.createListingLocalStore
import com.rotd.kejasafe.data.createUserProfileDataSource
import com.rotd.kejasafe.data.repoImpl.ApartmentChatRepositoryImpl
import com.rotd.kejasafe.data.repoImpl.createAuthRepository
import com.rotd.kejasafe.domain.biometric.BiometricAuthenticator
import com.rotd.kejasafe.domain.biometric.createBiometricAuthenticator
import com.rotd.kejasafe.domain.repository.ApartmentChatRepository
import com.rotd.kejasafe.domain.repository.AuthRepository
import com.rotd.kejasafe.domain.storage.SecureStorage
import com.rotd.kejasafe.domain.storage.createSecureStorage
import com.rotd.kejasafe.media.ChatAttachmentStore
import com.rotd.kejasafe.media.createChatAttachmentStore
import org.koin.dsl.module

val coreModule = module {
    single<ListingLocalStore> { createListingLocalStore() }
    single { ListingRepository(localStore = get()) }
    single { RentRepository() }
    single { ProfileRepository(authRepository = get()) }
    single<UserProfileDataSource> { createUserProfileDataSource() }
    single<BiometricAuthenticator> { createBiometricAuthenticator() }
    single<SecureStorage> { createSecureStorage() }
    single<AuthRepository> { createAuthRepository(get(), get(), get()) }
    single<ChatAttachmentStore> { createChatAttachmentStore() }
    single<ApartmentChatRepository> {
        ApartmentChatRepositoryImpl(listingRepository = get(), attachments = get())
    }
}
