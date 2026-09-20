package com.rotd.kejasafe.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.rotd.kejasafe.UserRole
import com.rotd.kejasafe.auth.LoginScreen
import com.rotd.kejasafe.auth.RegisterScreen
import com.rotd.kejasafe.chat.ChatInboxScreen
import com.rotd.kejasafe.chat.ChatThreadScreen
import com.rotd.kejasafe.components.KejaSafeNavTab
import com.rotd.kejasafe.components.destinationForTab
import com.rotd.kejasafe.components.homeDestinationForRole
import com.rotd.kejasafe.domain.chat.ApartmentChatPeer
import com.rotd.kejasafe.getPlatform
import com.rotd.kejasafe.listing.CreateListingScreen
import com.rotd.kejasafe.listing.LandlordBookingsScreen
import com.rotd.kejasafe.listing.LandlordHomeScreen
import com.rotd.kejasafe.listing.ListingAnalyticsScreen
import com.rotd.kejasafe.listing.ListingCalendarScreen
import com.rotd.kejasafe.listing.RequestPropertyScreen
import com.rotd.kejasafe.listing.RequestPropertyViewModel
import com.rotd.kejasafe.listing.TenantHomeScreen
import com.rotd.kejasafe.network.SessionManager
import com.rotd.kejasafe.profile.EditProfileScreen
import com.rotd.kejasafe.profile.ProfileScreen
import com.rotd.kejasafe.rent.RentScreen
import com.rotd.kejasafe.web.FeaturesScreen
import com.rotd.kejasafe.web.LandingScreen
import com.rotd.kejasafe.web.PolicyScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun KejaSafeNavHost(
    modifier: Modifier = Modifier,
    startDestination: KejaSafeDestination = KejaSafeDestination.Login
) {
    val backStack = remember { mutableStateListOf(startDestination) }
    val isWeb = getPlatform().isWeb
    /** Set when a visitor signs in to post a story, so auth returns them to the landing page. */
    val returnToLandingAfterAuth = remember { mutableStateOf(false) }

    fun currentRole(): UserRole? = SessionManager.currentUser.value?.role

    fun replaceRoot(destination: KejaSafeDestination) {
        if (backStack.isEmpty()) {
            backStack.add(destination)
            return
        }
        backStack[0] = destination
        while (backStack.size > 1) {
            backStack.removeAt(backStack.lastIndex)
        }
    }

    fun showLanding() {
        replaceRoot(KejaSafeDestination.Landing)
    }

    fun showLogin() {
        replaceRoot(KejaSafeDestination.Login)
    }

    fun showRegister() {
        replaceRoot(KejaSafeDestination.Register)
    }

    fun openAuth(destination: KejaSafeDestination) {
        if (backStack.lastOrNull() == destination) return
        backStack.add(destination)
    }

    /** Logout — web returns to landing; mobile to login. */
    fun navigateToLogin() {
        SessionManager.clearSession()
        if (isWeb) showLanding() else showLogin()
    }

    fun navigateHomeForRole() {
        if (!SessionManager.isLogged()) {
            navigateToLogin()
            return
        }
        if (returnToLandingAfterAuth.value) {
            returnToLandingAfterAuth.value = false
            showLanding()
            return
        }
        replaceRoot(homeDestinationForRole(currentRole()))
    }

    fun switchMainTab(tab: KejaSafeNavTab) {
        replaceRoot(destinationForTab(tab, currentRole()))
    }

    fun openDetail(destination: KejaSafeDestination) {
        backStack.add(destination)
    }

    fun goBack() {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
        }
    }

    /** Landing CTAs: signed-in visitors go straight to their dashboard, others to login. */
    fun openSignIn(returnToLanding: Boolean = false) {
        if (SessionManager.isLogged()) {
            if (returnToLanding) showLanding() else replaceRoot(homeDestinationForRole(currentRole()))
            return
        }
        returnToLandingAfterAuth.value = returnToLanding
        openAuth(KejaSafeDestination.Login)
    }

    val entryProvider = entryProvider<KejaSafeDestination> {
        entry<KejaSafeDestination.Landing> {
            LandingScreen(
                onNavigateToFeatures = { openDetail(KejaSafeDestination.Features) },
                onNavigateToPolicy = { openDetail(KejaSafeDestination.Policy) },
                onSignIn = { openSignIn() },
                onSignInToShareStory = { openSignIn(returnToLanding = true) },
            )
        }

        entry<KejaSafeDestination.Features> {
            FeaturesScreen(
                onNavigateToLanding = { showLanding() },
                onNavigateToPolicy = { 
                    if (backStack.isNotEmpty() && backStack.last() is KejaSafeDestination.Features) {
                        backStack.removeAt(backStack.lastIndex)
                    }
                    openDetail(KejaSafeDestination.Policy) 
                },
                onSignIn = { openSignIn() }
            )
        }

        entry<KejaSafeDestination.Policy> {
            PolicyScreen(
                onNavigateToLanding = { showLanding() },
                onNavigateToFeatures = { 
                    if (backStack.isNotEmpty() && backStack.last() is KejaSafeDestination.Policy) {
                        backStack.removeAt(backStack.lastIndex)
                    }
                    openDetail(KejaSafeDestination.Features) 
                },
                onSignIn = { openSignIn() }
            )
        }

        entry<KejaSafeDestination.Register> {
            RegisterScreen(
                onNavigateToLogin = {
                    if (backStack.size > 1) goBack() else showLogin()
                },
                onBack = if (isWeb && backStack.size > 1) {
                    { goBack() }
                } else null,
                onRegistrationSuccess = { navigateHomeForRole() }
            )
        }

        entry<KejaSafeDestination.Login> {
            LoginScreen(
                onNavigateToRegister = {
                    if (backStack.any { it is KejaSafeDestination.Register }) {
                        goBack()
                    } else {
                        openAuth(KejaSafeDestination.Register)
                    }
                },
                onBack = if (isWeb && backStack.size > 1) {
                    { goBack() }
                } else null,
                onLoginSuccess = { navigateHomeForRole() }
            )
        }

        entry<KejaSafeDestination.Home> {
            TenantHomeScreen(
                selectedTab = KejaSafeNavTab.HOME,
                onTabSelected = ::switchMainTab,
                onNavigateToProperty = { id -> openDetail(KejaSafeDestination.ListingCalendar(id)) },
                onNavigateToProfile = { switchMainTab(KejaSafeNavTab.PROFILE) },
                onNavigateToRequestProperty = { openDetail(KejaSafeDestination.RequestProperty) }
            )
        }

        entry<KejaSafeDestination.TenantHome> {
            TenantHomeScreen(
                selectedTab = KejaSafeNavTab.HOME,
                onTabSelected = ::switchMainTab,
                onNavigateToProperty = { id -> openDetail(KejaSafeDestination.ListingCalendar(id)) },
                onNavigateToProfile = { switchMainTab(KejaSafeNavTab.PROFILE) },
                onNavigateToRequestProperty = { openDetail(KejaSafeDestination.RequestProperty) }
            )
        }

        entry<KejaSafeDestination.RequestProperty> {
            val viewModel = koinViewModel<RequestPropertyViewModel>()
            RequestPropertyScreen(
                viewModel = viewModel,
                onBack = ::goBack
            )
        }

        entry<KejaSafeDestination.LandlordHome> {
            LandlordHomeScreen(
                selectedTab = KejaSafeNavTab.HOME,
                onTabSelected = ::switchMainTab,
                onNavigateToProfile = { switchMainTab(KejaSafeNavTab.PROFILE) },
                onNavigateToCreateListing = { openDetail(KejaSafeDestination.CreateListing()) },
                onNavigateToEditListing = { id -> openDetail(KejaSafeDestination.CreateListing(listingId = id)) },
                onNavigateToAnalytics = { id -> openDetail(KejaSafeDestination.ListingAnalytics(id)) },
                onNavigateToBookings = { openDetail(KejaSafeDestination.LandlordBookings) }
            )
        }

        entry<KejaSafeDestination.LandlordBookings> {
            LandlordBookingsScreen(
                onBack = ::goBack,
                onOpenLiveTour = { sessionId ->
                    openDetail(KejaSafeDestination.LiveTourCall(sessionId))
                }
            )
        }

        entry<KejaSafeDestination.LiveTourCall> { destination ->
            com.rotd.kejasafe.call.LiveTourCallScreen(
                sessionId = destination.sessionId,
                onBack = ::goBack
            )
        }

        entry<KejaSafeDestination.PropertyDetails> { destination ->
            ListingCalendarScreen(
                listingId = destination.id,
                onBack = ::goBack,
                onOpenLiveTour = { sessionId ->
                    openDetail(KejaSafeDestination.LiveTourCall(sessionId))
                }
            )
        }

        entry<KejaSafeDestination.ListingCalendar> { destination ->
            ListingCalendarScreen(
                listingId = destination.listingId,
                onBack = ::goBack,
                onOpenLiveTour = { sessionId ->
                    openDetail(KejaSafeDestination.LiveTourCall(sessionId))
                }
            )
        }

        entry<KejaSafeDestination.ListingAnalytics> { destination ->
            ListingAnalyticsScreen(
                listingId = destination.listingId,
                onBack = ::goBack,
                onEditListing = { id ->
                    if (backStack.isNotEmpty() && backStack.last() is KejaSafeDestination.ListingAnalytics) {
                        backStack.removeAt(backStack.lastIndex)
                    }
                    openDetail(KejaSafeDestination.CreateListing(listingId = id))
                }
            )
        }

        entry<KejaSafeDestination.Profile> {
            ProfileScreen(
                selectedTab = KejaSafeNavTab.PROFILE,
                onTabSelected = ::switchMainTab,
                onEditProfile = { openDetail(KejaSafeDestination.EditProfile) },
                onLogout = { navigateToLogin() },
                onCreateListingClick = { openDetail(KejaSafeDestination.CreateListing()) }
            )
        }

        entry<KejaSafeDestination.EditProfile> {
            EditProfileScreen(
                onBack = ::goBack,
                onSaved = {
                    if (backStack.isEmpty()) {
                        backStack.add(KejaSafeDestination.Profile)
                    } else {
                        backStack[0] = KejaSafeDestination.Profile
                        while (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                    }
                }
            )
        }

        entry<KejaSafeDestination.Rent> {
            RentScreen(
                selectedTab = KejaSafeNavTab.BILLS,
                onTabSelected = ::switchMainTab,
                onCreateListingClick = { openDetail(KejaSafeDestination.CreateListing()) }
            )
        }

        entry<KejaSafeDestination.Chat> {
            val role = currentRole()
            val isLandlord = role == UserRole.LANDLORD || role == UserRole.ADMIN
            ChatInboxScreen(
                selectedTab = KejaSafeNavTab.CHAT,
                onTabSelected = ::switchMainTab,
                onOpenThread = { peer ->
                    openDetail(
                        KejaSafeDestination.ChatThread(
                            peerUserId = peer.userId,
                            listingId = peer.listingId,
                            listingTitle = peer.listingTitle,
                            bookingId = peer.bookingId,
                            displayName = peer.displayName,
                            peerIsLandlord = peer.peerIsLandlord,
                        )
                    )
                },
                onOpenLiveTour = { sessionId ->
                    openDetail(KejaSafeDestination.LiveTourCall(sessionId))
                },
                onCreateListingClick = {
                    if (isLandlord) openDetail(KejaSafeDestination.CreateListing())
                }
            )
        }

        entry<KejaSafeDestination.ChatThread> { destination ->
            ChatThreadScreen(
                peer = ApartmentChatPeer(
                    userId = destination.peerUserId,
                    displayName = destination.displayName,
                    listingId = destination.listingId,
                    listingTitle = destination.listingTitle,
                    bookingId = destination.bookingId,
                    peerIsLandlord = destination.peerIsLandlord,
                ),
                onBack = ::goBack
            )
        }

        entry<KejaSafeDestination.CreateListing> { destination ->
            CreateListingScreen(
                listingId = destination.listingId,
                onBack = ::goBack,
                onListingSaved = {
                    if (backStack.isEmpty()) {
                        backStack.add(KejaSafeDestination.LandlordHome)
                    } else {
                        backStack[0] = KejaSafeDestination.LandlordHome
                        while (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                    }
                }
            )
        }
    }

    NavDisplay(
        modifier = modifier,
        backStack = backStack,
        onBack = {
            if (backStack.size > 1) {
                backStack.removeLastOrNull()
            }
        },
        entryProvider = entryProvider
    )
}
