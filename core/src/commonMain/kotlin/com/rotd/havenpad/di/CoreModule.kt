package com.rotd.havenpad.di

import com.rotd.havenpad.data.CommunityRepository
import com.rotd.havenpad.data.EscrowRepository
import com.rotd.havenpad.data.ListingLocalStore
import com.rotd.havenpad.data.ListingRepository
import com.rotd.havenpad.data.ProfileRepository
import com.rotd.havenpad.data.PropertyRequestRepository
import com.rotd.havenpad.data.RentRepository
import com.rotd.havenpad.data.UserProfileDataSource
import com.rotd.havenpad.data.createListingLocalStore
import com.rotd.havenpad.data.createUserProfileDataSource
import com.rotd.havenpad.data.repoImpl.ApartmentChatRepositoryImpl
import com.rotd.havenpad.data.repoImpl.createAuthRepository
import com.rotd.havenpad.domain.biometric.BiometricAuthenticator
import com.rotd.havenpad.domain.biometric.createBiometricAuthenticator
import com.rotd.havenpad.domain.repository.ApartmentChatRepository
import com.rotd.havenpad.domain.repository.AuthRepository
import com.rotd.havenpad.domain.storage.SecureStorage
import com.rotd.havenpad.domain.storage.createSecureStorage
import com.rotd.havenpad.media.ChatAttachmentStore
import com.rotd.havenpad.media.createChatAttachmentStore
import org.koin.dsl.module

val coreModule = module {
    single<ListingLocalStore> { createListingLocalStore() }
    single { ListingRepository(localStore = get()) }
    single { CommunityRepository() }
    single { RentRepository() }
    single { PropertyRequestRepository() }
    single { EscrowRepository() }
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
