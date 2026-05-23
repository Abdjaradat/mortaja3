package com.raed.app.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Message
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.raed.app.R
import com.raed.app.navigation.Screen
import com.raed.app.ui.screens.auth.AuthViewModel
import com.raed.app.ui.screens.clearance.ClearanceHomeContent
import com.raed.app.ui.screens.token.TokenViewModel

private val Gold = Color(0xFFC9A961)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    authViewModel: AuthViewModel,
    tokenViewModel: TokenViewModel = hiltViewModel(),
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tokenState by tokenViewModel.uiState.collectAsState()
    val listState = androidx.compose.foundation.lazy.rememberLazyListState()

    // Hide FAB when scrolled down
    var lastScrollIndex by remember { mutableIntStateOf(0) }
    var scrollingUp by remember { mutableStateOf(true) }
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemIndex }.collect { idx ->
            scrollingUp = idx <= lastScrollIndex
            lastScrollIndex = idx
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Mortaja3", fontWeight = FontWeight.Bold) },
                actions = {
                    TextButton(onClick = { navController.navigate(Screen.TokenWallet.route) }) {
                        Text(
                            "🪙 ${tokenState.balance}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Gold,
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = selectedTab == 0 && scrollingUp,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut(),
            ) {
                FloatingActionButton(
                    onClick = { navController.navigate(Screen.AddListingType.route) },
                    containerColor = Gold,
                    contentColor = Color.White,
                ) {
                    Icon(Icons.Outlined.Add, contentDescription = "نشر إعلان")
                }
            }
        },
        bottomBar = {
            NavigationBar {
                // Tab 0 — الإعلانات
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Outlined.Home, contentDescription = null) },
                    label = { Text(stringResource(R.string.listings)) },
                )
                // Tab 1 — الطلبات
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Outlined.Gavel, contentDescription = null) },
                    label = { Text(stringResource(R.string.requests)) },
                )
                // Tab 2 — الحاسبة
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = {
                        selectedTab = 2
                        navController.navigate(Screen.Calculator.route)
                    },
                    icon = { Icon(Icons.Outlined.Calculate, contentDescription = null) },
                    label = { Text(stringResource(R.string.calculator)) },
                )
                // Tab 3 — الرسائل
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.AutoMirrored.Outlined.Message, contentDescription = null) },
                    label = { Text(stringResource(R.string.messages)) },
                )
                // Tab 4 — الملف الشخصي
                NavigationBarItem(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    icon = { Icon(Icons.Outlined.Person, contentDescription = null) },
                    label = { Text(stringResource(R.string.profile)) },
                )
            }
        },
    ) { padding ->
        when (selectedTab) {
            0 -> {
                var listingsSubTab by rememberSaveable { mutableIntStateOf(0) }
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                ) {
                    TabRow(selectedTabIndex = listingsSubTab) {
                        Tab(selected = listingsSubTab == 0, onClick = { listingsSubTab = 0 }, text = { Text("مرتجع") })
                        Tab(selected = listingsSubTab == 1, onClick = { listingsSubTab = 1 }, text = { Text("سيارات") })
                        Tab(selected = listingsSubTab == 2, onClick = { listingsSubTab = 2 }, text = { Text("إعفاءات") })
                    }
                    when (listingsSubTab) {
                        0 -> ListingsContent(
                            category = "MORTAJA3",
                            onListingClick = { id -> navController.navigate(Screen.ListingDetail.createRoute("MORTAJA3", id)) },
                            onCalcClick = { price -> navController.navigate(Screen.Calculator.createRoute(price.toString())) },
                            listState = listState,
                            modifier = Modifier.weight(1f),
                        )
                        1 -> ListingsContent(
                            category = "REGULAR",
                            onListingClick = { id -> navController.navigate(Screen.ListingDetail.createRoute("REGULAR", id)) },
                            onCalcClick = { price -> navController.navigate(Screen.Calculator.createRoute(price.toString())) },
                            modifier = Modifier.weight(1f),
                        )
                        2 -> ListingsContent(
                            category = "EXEMPTION_RIGHT",
                            onListingClick = { id -> navController.navigate(Screen.ListingDetail.createRoute("EXEMPTION_RIGHT", id)) },
                            onCalcClick = { price -> navController.navigate(Screen.Calculator.createRoute(price.toString())) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            1 -> {
                var subTab by rememberSaveable { mutableIntStateOf(0) }
                androidx.compose.foundation.layout.Column(
                    modifier = Modifier.padding(padding).fillMaxSize(),
                ) {
                    TabRow(selectedTabIndex = subTab) {
                        Tab(selected = subTab == 0, onClick = { subTab = 0 }, text = { Text(stringResource(R.string.exemptions)) })
                        Tab(selected = subTab == 1, onClick = { subTab = 1 }, text = { Text(stringResource(R.string.clearance)) })
                    }
                    when (subTab) {
                        0 -> BrokerFeedContent(
                            onRequestClick = { id -> navController.navigate(Screen.Bid.createRoute(id)) },
                            modifier = Modifier.weight(1f),
                        )
                        1 -> ClearanceHomeContent(
                            onPostRequest = { navController.navigate(Screen.PostClearanceRequest.route) },
                            onRequestClick = { id -> navController.navigate(Screen.ClearanceRequestDetail.createRoute(id)) },
                            onRegisterAgent = { navController.navigate(Screen.RegisterAgent.route) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            3 -> ConversationsContent(
                onConversationClick = { id, name -> navController.navigate(Screen.Conversation.createRoute(id, name)) },
                modifier = Modifier.padding(padding),
            )
            4 -> ProfileContent(
                onNavigateToOfficerVerification = { navController.navigate(Screen.OfficerVerification.route) },
                onLoggedOut = {
                    authViewModel.resetToIdle()
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Home.route) { inclusive = true }
                    }
                },
                modifier = Modifier.padding(padding),
            )
        }
    }
}
