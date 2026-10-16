package com.rotd.havenpad.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.unit.dp
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
import com.rotd.havenpad.KycStatus
import com.rotd.havenpad.admin.AdminScreen
import com.rotd.havenpad.auth.LoginScreen
import com.rotd.havenpad.auth.RegisterScreen
import com.rotd.havenpad.auth.RoleChoiceScreen
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
import com.rotd.havenpad.profile.LandlordKycScreen
import com.rotd.havenpad.profile.StudentKycScreen
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
import com.rotd.havenpad.profile.SettingsScreen
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
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val isDesktopWeb = getPlatform().isWeb && maxWidth >= 720.dp
        val embedInDashboard = isDesktopWeb
        val backStack = remember { mutableStateListOf(startDestination) }
        val isWeb = getPlatform().isWeb
    val signedInUser by SessionManager.currentUser.collectAsState()
    LaunchedEffect(signedInUser?.id) {
        ChatNotificationInbox.showAccount(signedInUser?.id)
    }
    LaunchedEffect(signedInUser?.profilePictureUrl) {
        com.rotd.havenpad.media.ProfilePhotoLoader.load(signedInUser?.profilePictureUrl)
    }
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
        replaceRoot(HavenPadDestination.RoleChoice)
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
        if (currentRole() == UserRole.ADMIN) {
            replaceRoot(HavenPadDestination.Admin)
            return
        }
        if (!SessionManager.isLogged() && tab != HavenPadNavTab.HOME) {
            openAuth(HavenPadDestination.Login)
            return
        }
        replaceRoot(destinationForTab(tab, currentRole()))
    }

    fun requireSignIn(action: () -> Unit) {
        if (SessionManager.isLogged()) action() else openAuth(HavenPadDestination.Login)
    }

    fun textLandlord(listing: com.rotd.havenpad.PropertyListing) {
        if (!SessionManager.isLogged()) {
            openAuth(HavenPadDestination.Login)
            return
        }
        if (listing.landlordId.isBlank()) return
        ChatLaunchRequest.request(
            HavenPadDestination.ChatThread(
                peerUserId = listing.landlordId,
                listingId = listing.id,
                listingTitle = listing.title,
                bookingId = "",
                displayName = "Landlord",
                peerIsLandlord = true,
            )
        )
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

    fun browseHouses() {
        if (SessionManager.isLogged()) navigateHomeForRole()
        else replaceRoot(HavenPadDestination.TenantHome)
    }

    fun openDetail(destination: HavenPadDestination) {
        backStack.add(destination)
    }

    fun openChatFromNotice(notice: ChatNotice) {
        ChatNotificationInbox.markRead(notice.id)
        if (notice.peerUserId.isBlank()) {
            openDetail(HavenPadDestination.Rent)
            return
        }
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

    fun openKyc() {
        val role = SessionManager.currentUser.value?.role
        if (role == UserRole.STUDENT) {
            openDetail(HavenPadDestination.StudentKyc(allowSkip = false))
        } else if (role == UserRole.LANDLORD) {
            openDetail(HavenPadDestination.LandlordKyc(allowSkip = false))
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

    val pendingBill by com.rotd.havenpad.navigation.BillLaunchRequest.pending.collectAsState()
    LaunchedEffect(pendingBill) {
        val billId = pendingBill ?: return@LaunchedEffect
        if (!SessionManager.isLogged()) return@LaunchedEffect
        com.rotd.havenpad.navigation.BillLaunchRequest.consume()
        openDetail(HavenPadDestination.Rent)
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
                onExploreListings = { browseHouses() },
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

        entry<HavenPadDestination.RoleChoice> {
            RoleChoiceScreen(
                onBack = {
                    if (backStack.size > 1) goBack() else showLogin()
                },
                onContinue = { role ->
                    openAuth(HavenPadDestination.Register(role.name))
                },
            )
        }

        entry<HavenPadDestination.Register> { destination ->
            val role = runCatching { UserRole.valueOf(destination.roleName) }
                .getOrDefault(UserRole.STUDENT)
            RegisterScreen(
                role = role,
                onNavigateToLogin = {
                    if (backStack.size > 1) goBack() else showLogin()
                },
                onBack = if (backStack.size > 1) {
                    { goBack() }
                } else null,
                onRegistrationSuccess = {
                    val user = SessionManager.currentUser.value
                    if (user?.kycStatus == KycStatus.NOT_SUBMITTED && user.role == UserRole.LANDLORD) {
                        replaceRoot(HavenPadDestination.LandlordKyc(allowSkip = true))
                    } else if (user?.kycStatus == KycStatus.NOT_SUBMITTED && user.role == UserRole.STUDENT) {
                        replaceRoot(HavenPadDestination.StudentKyc(allowSkip = true))
                    } else {
                        navigateHomeForRole()
                    }
                }
            )
        }

        entry<HavenPadDestination.Login> {
            LoginScreen(
                onNavigateToRegister = {
                    if (backStack.any { it is HavenPadDestination.RoleChoice }) {
                        goBack()
                    } else {
                        openAuth(HavenPadDestination.RoleChoice)
                    }
                },
                onBack = if (isWeb && backStack.size > 1) {
                    { goBack() }
                } else null,
                onContinueAsGuest = { replaceRoot(HavenPadDestination.TenantHome) },
                onLoginSuccess = { navigateHomeForRole() }
            )
        }

        entry<HavenPadDestination.Home> {
            webDashboard(HavenPadNavTab.HOME) {
                TenantHomeScreen(
                    selectedTab = HavenPadNavTab.HOME,
                    onTabSelected = ::switchMainTab,
                    onNavigateToProperty = { id ->
                        requireSignIn { openDetail(HavenPadDestination.ListingCalendar(id)) }
                    },
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
                    onNavigateToRequestProperty = { requireSignIn { openDetail(HavenPadDestination.RequestProperty) } },
                    onNavigateToNotifications = { requireSignIn { openDetail(HavenPadDestination.Notifications) } },
                    onTextLandlord = ::textLandlord,
                    signedIn = SessionManager.isLogged(),
                    embedInDashboard = embedInDashboard,
                )
            }
        }

        entry<HavenPadDestination.TenantHome> {
            webDashboard(HavenPadNavTab.HOME) {
                TenantHomeScreen(
                    selectedTab = HavenPadNavTab.HOME,
                    onTabSelected = ::switchMainTab,
                    onNavigateToProperty = { id ->
                        requireSignIn { openDetail(HavenPadDestination.ListingCalendar(id)) }
                    },
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
                    onNavigateToRequestProperty = { requireSignIn { openDetail(HavenPadDestination.RequestProperty) } },
                    onNavigateToNotifications = { requireSignIn { openDetail(HavenPadDestination.Notifications) } },
                    onTextLandlord = ::textLandlord,
                    signedIn = SessionManager.isLogged(),
                    embedInDashboard = embedInDashboard,
                )
            }
        }

        entry<HavenPadDestination.ListingSearch> { destination ->
            ListingSearchScreen(
                onBack = ::goBack,
                onNavigateToProperty = { id ->
                    requireSignIn { openDetail(HavenPadDestination.ListingCalendar(id)) }
                },
                onTextLandlord = ::textLandlord,
                signedIn = SessionManager.isLogged(),
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
                    onOpenSettings = { openDetail(HavenPadDestination.Settings) },
                    onOpenHome = { switchMainTab(HavenPadNavTab.HOME) },
                    onOpenBookings = { openDetail(HavenPadDestination.LandlordBookings) },
                    onOpenKyc = { openKyc() },
                    onOpenAdmin = { replaceRoot(HavenPadDestination.Admin) },
                    onCreateListingClick = { openDetail(HavenPadDestination.CreateListing()) }
                )
            }
        }

        entry<HavenPadDestination.StudentKyc> { destination ->
            webDashboard(HavenPadNavTab.PROFILE) {
                StudentKycScreen(
                    allowSkip = destination.allowSkip,
                    onBack = {
                        if (destination.allowSkip) navigateHomeForRole() else goBack()
                    },
                    onFinished = {
                        if (destination.allowSkip || backStack.size <= 1) navigateHomeForRole()
                        else goBack()
                    },
                )
            }
        }

        entry<HavenPadDestination.LandlordKyc> { destination ->
            webDashboard(HavenPadNavTab.PROFILE) {
                LandlordKycScreen(
                    allowSkip = destination.allowSkip,
                    onBack = {
                        if (destination.allowSkip) navigateHomeForRole() else goBack()
                    },
                    onFinished = {
                        if (destination.allowSkip || backStack.size <= 1) navigateHomeForRole()
                        else goBack()
                    },
                )
            }
        }

        entry<HavenPadDestination.Admin> {
            webDashboard(HavenPadNavTab.PROFILE) {
                AdminScreen(onLogout = ::navigateToLogin)
            }
        }

        entry<HavenPadDestination.Settings> {
            webDashboard(HavenPadNavTab.PROFILE) {
                SettingsScreen(
                    onBack = ::goBack,
                    onEditProfile = { openDetail(HavenPadDestination.EditProfile) },
                    onOpenNotifications = { openDetail(HavenPadDestination.Notifications) },
                    onOpenKyc = { openKyc() },
                    onOpenAdmin = { replaceRoot(HavenPadDestination.Admin) },
                    onLogout = { navigateToLogin() },
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
                embedInDashboard = embedInDashboard,
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
                onOpenVerification = {
                    openDetail(HavenPadDestination.LandlordKyc(allowSkip = false))
                },
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
    val showWebPanel = isDesktopWeb && when (topDestination) {
        null,
        HavenPadDestination.Landing,
        HavenPadDestination.Features,
        HavenPadDestination.Policy,
        HavenPadDestination.Login,
        HavenPadDestination.RoleChoice -> false
        is HavenPadDestination.Register -> false
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
                HavenPadDestination.EditProfile,
                HavenPadDestination.Settings,
                is HavenPadDestination.LandlordKyc,
                is HavenPadDestination.StudentKyc,
                HavenPadDestination.Admin -> HavenPadNavTab.PROFILE
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
                onOpenBookings = {
                    if (SessionManager.isLogged()) replaceRoot(HavenPadDestination.LandlordBookings)
                    else openAuth(HavenPadDestination.Login)
                },
                onOpenAdmin = { replaceRoot(HavenPadDestination.Admin) },
                bookingsSelected = topDestination is HavenPadDestination.LandlordBookings,
                toursSelected = topDestination is HavenPadDestination.LiveTourCall,
                adminSelected = topDestination is HavenPadDestination.Admin,
                onOpenLanding = { showLanding() },
                onLogout = {
                    if (SessionManager.isLogged()) navigateToLogin()
                    else openAuth(HavenPadDestination.Login)
                },
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
}
