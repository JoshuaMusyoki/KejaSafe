package com.rotd.havenpad.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.rotd.havenpad.chat.ChatNotificationInbox
import com.rotd.havenpad.chat.ChatPanelLaunch
import com.rotd.havenpad.notify.consumeClickedChatNotification
import kotlinx.coroutines.delay
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import com.rotd.havenpad.UserRole
import com.rotd.havenpad.auth.LoginScreen
import com.rotd.havenpad.auth.RegisterScreen
import com.rotd.havenpad.chat.ChatIncomingNotifications
import com.rotd.havenpad.chat.ChatInboxScreen
import com.rotd.havenpad.chat.ChatInboxTab
import com.rotd.havenpad.chat.ChatNotice
import com.rotd.havenpad.chat.ChatSidebarRequest
import com.rotd.havenpad.chat.toPeer
import com.rotd.havenpad.chat.NotificationsScreen
import com.rotd.havenpad.chat.ChatThreadScreen
import com.rotd.havenpad.chat.HavenPadWebDashboardShell
import com.rotd.havenpad.components.HavenPadNavTab
import com.rotd.havenpad.components.destinationForTab
import com.rotd.havenpad.components.tabForDestination
import com.rotd.havenpad.components.homeDestinationForRole
import com.rotd.havenpad.domain.chat.ApartmentChatPeer
import com.rotd.havenpad.getPlatform
import com.rotd.havenpad.listing.CreateListingScreen
import com.rotd.havenpad.listing.LandlordBookingsScreen
import com.rotd.havenpad.listing.LandlordHomeScreen
import com.rotd.havenpad.listing.ListingAnalyticsScreen
import com.rotd.havenpad.listing.ListingCalendarScreen
import com.rotd.havenpad.listing.ListingSearchScreen
import com.rotd.havenpad.listing.RequestPropertyScreen
import com.rotd.havenpad.listing.TenantHomeScreen
import com.rotd.havenpad.listing.viewModel.RequestPropertyViewModel
import com.rotd.havenpad.network.SessionManager
import com.rotd.havenpad.profile.EditProfileScreen
import com.rotd.havenpad.profile.ProfileScreen
import com.rotd.havenpad.rent.RentScreen
import com.rotd.havenpad.web.FeaturesScreen
import com.rotd.havenpad.web.LandingScreen
import com.rotd.havenpad.web.PolicyScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HavenPadNavHost(
    modifier: Modifier = Modifier,
    startDestination: HavenPadDestination = HavenPadDestination.Login
) {
    val backStack = remember { mutableStateListOf(startDestination) }
    val isWeb = getPlatform().isWeb
    val signedInUser by SessionManager.currentUser.collectAsState()
    if (signedInUser != null) {
        ChatIncomingNotifications()
    }
    /** Set when a visitor signs in to post a story, so auth returns them to the landing page. */
    val returnToLandingAfterAuth = remember { mutableStateOf(false) }

    fun currentRole(): UserRole? = SessionManager.currentUser.value?.role

    fun replaceRoot(destination: HavenPadDestination) {
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
        replaceRoot(HavenPadDestination.Landing)
    }

    fun showLogin() {
        replaceRoot(HavenPadDestination.Login)
    }

    fun showRegister() {
        replaceRoot(HavenPadDestination.Register)
    }

    fun openAuth(destination: HavenPadDestination) {
        if (backStack.lastOrNull() == destination) return
        backStack.add(destination)
    }

    /** Logout — web returns to landing; mobile to login. */
    fun navigateToLogin() {
        SessionManager.clearSession()
        if (isWeb) showLanding() else showLogin()
    }

    fun switchMainTab(tab: HavenPadNavTab) {
        replaceRoot(destinationForTab(tab, currentRole()))
    }

    @Composable
    fun webDashboard(tab: HavenPadNavTab, content: @Composable () -> Unit) {
        content()
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

    fun openDetail(destination: HavenPadDestination) {
        backStack.add(destination)
    }

    fun openChatFromNotice(notice: ChatNotice) {
        ChatNotificationInbox.markRead(notice.id)
        if (isWeb) {
            ChatPanelLaunch.open(notice.toPeer())
            ChatSidebarRequest.select(ChatInboxTab.MESSAGES)
            replaceRoot(HavenPadDestination.Chat)
            return
        }
        openDetail(
            HavenPadDestination.ChatThread(
                peerUserId = notice.peerUserId,
                listingId = notice.listingId,
                listingTitle = notice.listingTitle,
                bookingId = notice.bookingId,
                displayName = notice.displayName.ifBlank { notice.title },
                peerIsLandlord = notice.peerIsLandlord,
            )
        )
    }

    fun goBack() {
        if (backStack.size > 1) {
            backStack.removeLastOrNull()
        }
    }

    val pendingChat by ChatLaunchRequest.pending.collectAsState()
    LaunchedEffect(pendingChat) {
        val thread = pendingChat ?: return@LaunchedEffect
        if (!SessionManager.isLogged()) return@LaunchedEffect
        ChatLaunchRequest.consume()
        if (isWeb) {
            ChatPanelLaunch.open(
                ApartmentChatPeer(
                    userId = thread.peerUserId,
                    displayName = thread.displayName.ifBlank { "HavenPad" },
                    listingId = thread.listingId,
                    listingTitle = thread.listingTitle,
                    bookingId = thread.bookingId,
                    peerIsLandlord = thread.peerIsLandlord,
                )
            )
            ChatSidebarRequest.select(ChatInboxTab.MESSAGES)
            replaceRoot(HavenPadDestination.Chat)
        } else {
            openDetail(thread)
        }
    }

    if (isWeb && signedInUser != null) {
        LaunchedEffect(signedInUser?.id) {
            while (true) {
                val id = consumeClickedChatNotification()
                if (id.isNotBlank()) {
                    val notice = ChatNotificationInbox.items.value.firstOrNull { it.id == id }
                    if (notice != null) {
                        openChatFromNotice(notice)
                    } else {
                        ChatNotificationInbox.markRead(id)
                    }
                }
                delay(400)
            }
        }
    }

    val pendingTour by TourLaunchRequest.pending.collectAsState()
    LaunchedEffect(pendingTour) {
        val sessionId = pendingTour ?: return@LaunchedEffect
        if (!SessionManager.isLogged()) return@LaunchedEffect
        TourLaunchRequest.consume()
        openDetail(HavenPadDestination.LiveTourCall(sessionId))
    }

    /** Landing CTAs: signed-in visitors go straight to their dashboard, others to login. */
    fun openSignIn(returnToLanding: Boolean = false) {
        if (SessionManager.isLogged()) {
            if (returnToLanding) showLanding() else replaceRoot(homeDestinationForRole(currentRole()))
            return
        }
        returnToLandingAfterAuth.value = returnToLanding
        openAuth(HavenPadDestination.Login)
    }

    val entryProvider = entryProvider<HavenPadDestination> {
        entry<HavenPadDestination.Landing> {
            LandingScreen(
                onNavigateToFeatures = { openDetail(HavenPadDestination.Features) },
                onNavigateToPolicy = { openDetail(HavenPadDestination.Policy) },
                onSignIn = { openSignIn() },
                onSignInToShareStory = { openSignIn(returnToLanding = true) },
            )
        }

        entry<HavenPadDestination.Features> {
            FeaturesScreen(
                onNavigateToLanding = { showLanding() },
                onNavigateToPolicy = { 
                    if (backStack.isNotEmpty() && backStack.last() is HavenPadDestination.Features) {
                        backStack.removeAt(backStack.lastIndex)
                    }
                    openDetail(HavenPadDestination.Policy) 
                },
                onSignIn = { openSignIn() }
            )
        }

        entry<HavenPadDestination.Policy> {
            PolicyScreen(
                onNavigateToLanding = { showLanding() },
                onNavigateToFeatures = { 
                    if (backStack.isNotEmpty() && backStack.last() is HavenPadDestination.Policy) {
                        backStack.removeAt(backStack.lastIndex)
                    }
                    openDetail(HavenPadDestination.Features) 
                },
                onSignIn = { openSignIn() }
            )
        }

        entry<HavenPadDestination.Register> {
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

        entry<HavenPadDestination.Login> {
            LoginScreen(
                onNavigateToRegister = {
                    if (backStack.any { it is HavenPadDestination.Register }) {
                        goBack()
                    } else {
                        openAuth(HavenPadDestination.Register)
                    }
                },
                onBack = if (isWeb && backStack.size > 1) {
                    { goBack() }
                } else null,
                onLoginSuccess = { navigateHomeForRole() }
            )
        }

        entry<HavenPadDestination.Home> {
            webDashboard(HavenPadNavTab.HOME) {
                TenantHomeScreen(
                    selectedTab = HavenPadNavTab.HOME,
                    onTabSelected = ::switchMainTab,
                    onNavigateToProperty = { id -> openDetail(HavenPadDestination.ListingCalendar(id)) },
                    onNavigateToProfile = { switchMainTab(HavenPadNavTab.PROFILE) },
                    onNavigateToSearch = { query, lat, lng ->
                        openDetail(
                            HavenPadDestination.ListingSearch(
                                query = query,
                                latitude = lat,
                                longitude = lng
                            )
                        )
                    },
                    onNavigateToRequestProperty = { openDetail(HavenPadDestination.RequestProperty) },
                    onNavigateToNotifications = { openDetail(HavenPadDestination.Notifications) }
                )
            }
        }

        entry<HavenPadDestination.TenantHome> {
            webDashboard(HavenPadNavTab.HOME) {
                TenantHomeScreen(
                    selectedTab = HavenPadNavTab.HOME,
                    onTabSelected = ::switchMainTab,
                    onNavigateToProperty = { id -> openDetail(HavenPadDestination.ListingCalendar(id)) },
                    onNavigateToProfile = { switchMainTab(HavenPadNavTab.PROFILE) },
                    onNavigateToSearch = { query, lat, lng ->
                        openDetail(
                            HavenPadDestination.ListingSearch(
                                query = query,
                                latitude = lat,
                                longitude = lng
                            )
                        )
                    },
                    onNavigateToRequestProperty = { openDetail(HavenPadDestination.RequestProperty) },
                    onNavigateToNotifications = { openDetail(HavenPadDestination.Notifications) }
                )
            }
        }

        entry<HavenPadDestination.ListingSearch> { destination ->
            ListingSearchScreen(
                onBack = ::goBack,
                onNavigateToProperty = { id -> openDetail(HavenPadDestination.ListingCalendar(id)) },
                initialQuery = destination.query,
                initialLatitude = destination.latitude,
                initialLongitude = destination.longitude
            )
        }

        entry<HavenPadDestination.RequestProperty> {
            val viewModel = koinViewModel<RequestPropertyViewModel>()
            RequestPropertyScreen(
                viewModel = viewModel,
                onBack = ::goBack
            )
        }

        entry<HavenPadDestination.LandlordHome> {
            webDashboard(HavenPadNavTab.HOME) {
                LandlordHomeScreen(
                    selectedTab = HavenPadNavTab.HOME,
                    onTabSelected = ::switchMainTab,
                    onNavigateToProfile = { switchMainTab(HavenPadNavTab.PROFILE) },
                    onNavigateToCreateListing = { openDetail(HavenPadDestination.CreateListing()) },
                    onNavigateToEditListing = { id -> openDetail(HavenPadDestination.CreateListing(listingId = id)) },
                    onNavigateToAnalytics = { id -> openDetail(HavenPadDestination.ListingAnalytics(id)) },
                    onNavigateToBookings = { openDetail(HavenPadDestination.LandlordBookings) },
                    onNavigateToNotifications = { openDetail(HavenPadDestination.Notifications) }
                )
            }
        }

        entry<HavenPadDestination.LandlordBookings> {
            webDashboard(HavenPadNavTab.HOME) {
                LandlordBookingsScreen(
                    onBack = ::goBack,
                    onOpenLiveTour = { sessionId ->
                        openDetail(HavenPadDestination.LiveTourCall(sessionId))
                    }
                )
            }
        }

        entry<HavenPadDestination.LiveTourCall> { destination ->
            webDashboard(HavenPadNavTab.CHAT) {
                com.rotd.havenpad.call.LiveTourCallScreen(
                    sessionId = destination.sessionId,
                    onBack = ::goBack
                )
            }
        }

        entry<HavenPadDestination.PropertyDetails> { destination ->
            ListingCalendarScreen(
                listingId = destination.id,
                onBack = ::goBack,
                onOpenLiveTour = { sessionId ->
                    openDetail(HavenPadDestination.LiveTourCall(sessionId))
                }
            )
        }

        entry<HavenPadDestination.ListingCalendar> { destination ->
            ListingCalendarScreen(
                listingId = destination.listingId,
                onBack = ::goBack,
                onOpenLiveTour = { sessionId ->
                    openDetail(HavenPadDestination.LiveTourCall(sessionId))
                }
            )
        }

        entry<HavenPadDestination.ListingAnalytics> { destination ->
            ListingAnalyticsScreen(
                listingId = destination.listingId,
                onBack = ::goBack,
                onEditListing = { id ->
                    if (backStack.isNotEmpty() && backStack.last() is HavenPadDestination.ListingAnalytics) {
                        backStack.removeAt(backStack.lastIndex)
                    }
                    openDetail(HavenPadDestination.CreateListing(listingId = id))
                }
            )
        }

        entry<HavenPadDestination.Profile> {
            webDashboard(HavenPadNavTab.PROFILE) {
                ProfileScreen(
                    selectedTab = HavenPadNavTab.PROFILE,
                    onTabSelected = ::switchMainTab,
                    onEditProfile = { openDetail(HavenPadDestination.EditProfile) },
                    onOpenNotifications = { openDetail(HavenPadDestination.Notifications) },
                    onOpenHome = { switchMainTab(HavenPadNavTab.HOME) },
                    onOpenChat = { switchMainTab(HavenPadNavTab.CHAT) },
                    onOpenBills = { switchMainTab(HavenPadNavTab.BILLS) },
                    onOpenBookings = { openDetail(HavenPadDestination.LandlordBookings) },
                    onLogout = { navigateToLogin() },
                    onCreateListingClick = { openDetail(HavenPadDestination.CreateListing()) }
                )
            }
        }

        entry<HavenPadDestination.EditProfile> {
            EditProfileScreen(
                onBack = ::goBack,
                onSaved = {
                    if (backStack.isEmpty()) {
                        backStack.add(HavenPadDestination.Profile)
                    } else {
                        backStack[0] = HavenPadDestination.Profile
                        while (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                    }
                }
            )
        }

        entry<HavenPadDestination.Rent> {
            webDashboard(HavenPadNavTab.BILLS) {
                RentScreen(
                    selectedTab = HavenPadNavTab.BILLS,
                    onTabSelected = ::switchMainTab,
                    onCreateListingClick = { openDetail(HavenPadDestination.CreateListing()) }
                )
            }
        }

        entry<HavenPadDestination.Chat> {
            val role = currentRole()
            val isLandlord = role == UserRole.LANDLORD || role == UserRole.ADMIN
            ChatInboxScreen(
                selectedTab = HavenPadNavTab.CHAT,
                onTabSelected = ::switchMainTab,
                embedInDashboard = isWeb,
                onOpenBookings = { replaceRoot(HavenPadDestination.LandlordBookings) },
                onOpenThread = { peer ->
                    openDetail(
                        HavenPadDestination.ChatThread(
                            peerUserId = peer.userId,
                            listingId = peer.listingId,
                            listingTitle = peer.listingTitle,
                            bookingId = peer.bookingId,
                            displayName = peer.displayName,
                            peerIsLandlord = peer.peerIsLandlord,
                        )
                    )
                },
                onOpenNotifications = { openDetail(HavenPadDestination.Notifications) },
                onOpenLiveTour = { sessionId ->
                    openDetail(HavenPadDestination.LiveTourCall(sessionId))
                },
                onCreateListingClick = {
                    if (isLandlord) openDetail(HavenPadDestination.CreateListing())
                }
            )
        }

        entry<HavenPadDestination.ChatThread> { destination ->
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

        entry<HavenPadDestination.Notifications> {
            webDashboard(HavenPadNavTab.CHAT) {
                NotificationsScreen(
                    onBack = ::goBack,
                    onOpenNotice = { notice -> openChatFromNotice(notice) },
                )
            }
        }

        entry<HavenPadDestination.CreateListing> { destination ->
            CreateListingScreen(
                listingId = destination.listingId,
                onBack = ::goBack,
                onListingSaved = {
                    if (backStack.isEmpty()) {
                        backStack.add(HavenPadDestination.LandlordHome)
                    } else {
                        backStack[0] = HavenPadDestination.LandlordHome
                        while (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
                    }
                }
            )
        }
    }

    val topDestination = backStack.lastOrNull()
    val showWebPanel = isWeb && signedInUser != null && when (topDestination) {
        null,
        HavenPadDestination.Landing,
        HavenPadDestination.Features,
        HavenPadDestination.Policy,
        HavenPadDestination.Login,
        HavenPadDestination.Register -> false
        else -> true
    }
    val sidebarChatTab by ChatSidebarRequest.tab.collectAsState()

    @Composable
    fun AppNavigation() {
        NavDisplay(
            modifier = Modifier.fillMaxSize(),
            backStack = backStack,
            onBack = {
                if (backStack.size > 1) {
                    backStack.removeLastOrNull()
                }
            },
            entryProvider = entryProvider
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        if (showWebPanel) {
            val panelTab = when (topDestination) {
                HavenPadDestination.Chat,
                is HavenPadDestination.ChatThread,
                HavenPadDestination.Notifications,
                is HavenPadDestination.LiveTourCall -> HavenPadNavTab.CHAT
                HavenPadDestination.Rent -> HavenPadNavTab.BILLS
                HavenPadDestination.Profile,
                HavenPadDestination.EditProfile -> HavenPadNavTab.PROFILE
                else -> tabForDestination(topDestination ?: HavenPadDestination.Home) ?: HavenPadNavTab.HOME
            }
            HavenPadWebDashboardShell(
                currentTab = panelTab,
                currentChatTab = sidebarChatTab,
                onTabSelected = ::switchMainTab,
                onChatTabSelected = { tab ->
                    ChatSidebarRequest.select(tab)
                    switchMainTab(HavenPadNavTab.CHAT)
                },
                onOpenBookings = { replaceRoot(HavenPadDestination.LandlordBookings) },
                bookingsSelected = topDestination is HavenPadDestination.LandlordBookings,
                toursSelected = topDestination is HavenPadDestination.LiveTourCall,
                onLogout = ::navigateToLogin,
            ) {
                AppNavigation()
            }
        } else {
            AppNavigation()
        }
        if (signedInUser != null) {
            com.rotd.havenpad.call.IncomingCallOverlay { sessionId ->
                openDetail(HavenPadDestination.LiveTourCall(sessionId))
            }
        }
    }
}
