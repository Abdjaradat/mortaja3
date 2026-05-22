package com.raed.app.navigation

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.raed.app.ui.screens.*
import com.raed.app.ui.screens.auth.*
import com.raed.app.ui.screens.listing.*
import com.raed.app.ui.screens.token.TokenWalletScreen

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Login : Screen("auth/login")
    object UserType : Screen("auth/user-type")
    object ProfileSetup : Screen("auth/profile-setup")
    object Home : Screen("home")

    // Calculator accepts an optional pre-filled exemption cost via query param
    object Calculator : Screen("calculator") {
        const val ROUTE_WITH_ARG = "calculator?exemptionCost={exemptionCost}"
        fun createRoute(exemptionCost: String) = "calculator?exemptionCost=$exemptionCost"
    }

    // Detail carries both type ("car" / "exemption") and id
    object ListingDetail : Screen("listing/{listingType}/{listingId}") {
        fun createRoute(listingType: String, listingId: String) = "listing/$listingType/$listingId"
    }

    object Conversation : Screen("conversation/{conversationId}") {
        const val ROUTE_WITH_NAME = "conversation/{conversationId}?name={name}"
        fun createRoute(id: String, name: String = "") = "conversation/$id?name=${Uri.encode(name)}"
    }
    object OfficerVerification : Screen("officer-verification")
    object TokenWallet : Screen("token-wallet")
    object AddListingType : Screen("add-listing/type")
    object AddCarListing : Screen("add-listing/car")
    object AddExemptionListing : Screen("add-listing/exemption")
    object PostRequest : Screen("post-request")
    object Bid : Screen("bid/{requestId}") {
        fun createRoute(id: String) = "bid/$id"
    }
}

@Composable
fun RaedNavGraph(navController: NavHostController = rememberNavController()) {
    val authViewModel: AuthViewModel = hiltViewModel()

    NavHost(navController = navController, startDestination = Screen.Splash.route) {

        composable(Screen.Splash.route) {
            SplashScreen(
                onNavigateToAuth = {
                    navController.navigate(Screen.Login.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
                onNavigateToHome = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.Login.route) {
            LoginScreen(
                viewModel = authViewModel,
                onNeedsUserType = { navController.navigate(Screen.UserType.route) },
                onNeedsProfile = {
                    navController.navigate(Screen.ProfileSetup.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onAuthenticated = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.UserType.route) {
            UserTypeScreen(
                viewModel = authViewModel,
                onNeedsProfile = {
                    navController.navigate(Screen.ProfileSetup.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
                onAuthenticated = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.ProfileSetup.route) {
            ProfileSetupScreen(
                viewModel = authViewModel,
                onComplete = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Login.route) { inclusive = true }
                    }
                },
            )
        }

        composable(Screen.Home.route) {
            HomeScreen(navController = navController, authViewModel = authViewModel)
        }

        composable(
            route = Screen.Calculator.ROUTE_WITH_ARG,
            arguments = listOf(
                navArgument("exemptionCost") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = true
                },
            ),
        ) { backStackEntry ->
            val prefilled = backStackEntry.arguments?.getString("exemptionCost") ?: ""
            CalculatorScreen(prefilledExemptionCost = prefilled)
        }

        composable(
            route = Screen.ListingDetail.route,
            arguments = listOf(
                navArgument("listingType") { type = NavType.StringType },
                navArgument("listingId") { type = NavType.StringType },
            ),
        ) {
            ListingDetailScreen(
                onBack = { navController.popBackStack() },
                onNavigateToCalculator = { price ->
                    navController.navigate(Screen.Calculator.createRoute(price.toString()))
                },
                onNavigateToWallet = { navController.navigate(Screen.TokenWallet.route) },
                onNavigateToConversation = { id, name ->
                    navController.navigate(Screen.Conversation.createRoute(id, name))
                },
            )
        }

        composable(
            route = Screen.Conversation.ROUTE_WITH_NAME,
            arguments = listOf(
                navArgument("conversationId") { type = NavType.StringType },
                navArgument("name") {
                    type = NavType.StringType
                    defaultValue = ""
                    nullable = true
                },
            ),
        ) { backStackEntry ->
            val name = backStackEntry.arguments?.getString("name") ?: ""
            ConversationScreen(
                otherUserName = name,
                onBack = { navController.popBackStack() },
            )
        }

        composable(Screen.OfficerVerification.route) {
            OfficerVerificationScreen(
                onBack = { navController.popBackStack() },
                onSuccess = { navController.popBackStack() },
            )
        }

        composable(Screen.TokenWallet.route) {
            TokenWalletScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.AddListingType.route) {
            AddListingTypeScreen(
                onBack = { navController.popBackStack() },
                onAddCar = { navController.navigate(Screen.AddCarListing.route) },
                onAddExemption = { navController.navigate(Screen.AddExemptionListing.route) },
                onPostRequest = { navController.navigate(Screen.PostRequest.route) },
            )
        }

        composable(Screen.AddCarListing.route) {
            AddCarListingScreen(
                onBack = { navController.popBackStack() },
                onSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                },
                onNavigateToWallet = { navController.navigate(Screen.TokenWallet.route) },
            )
        }

        composable(Screen.AddExemptionListing.route) {
            AddExemptionListingScreen(
                onBack = { navController.popBackStack() },
                onSuccess = {
                    navController.navigate(Screen.Home.route) {
                        popUpTo(Screen.Home.route) { inclusive = false }
                    }
                },
                onNavigateToWallet = { navController.navigate(Screen.TokenWallet.route) },
            )
        }

        composable(Screen.PostRequest.route) {
            PostRequestScreen(onBack = { navController.popBackStack() })
        }

        composable(
            route = Screen.Bid.route,
            arguments = listOf(navArgument("requestId") { type = NavType.StringType }),
        ) {
            BidScreen(onBack = { navController.popBackStack() })
        }
    }
}
