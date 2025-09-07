package com.example.homestay

import AddPriceScreen
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.homestay.data.local.HomestayDatabase
import com.example.homestay.data.repository.FirebaseRepository
import com.example.homestay.data.repository.HomestayRepository
import com.example.homestay.data.repository.PropertyListingRepository
import com.example.homestay.ui.HostHome.HomeWithDetailsViewModel
import com.example.homestay.ui.HostHome.HomeScreen
import com.example.homestay.ui.HostHome.HomeScreenWrapper
import com.example.homestay.ui.addPromo.AddPromoScreen
import com.example.homestay.ui.property.HostAddOrEditHomeScreen
import com.example.homestay.ui.property.PropertyListingViewModel
import com.google.firebase.FirebaseApp
import java.net.URLDecoder
import java.net.URLEncoder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        val context = this

        // --- Repositories ---
        val database = HomestayDatabase.getDatabase(context)
        val homestayRepo = HomestayRepository(
            database.homestayPriceDao(),
            database.promotionDao()
        )
        val propertyRepo = PropertyListingRepository(
            homeDao = database.HomeDao(),
            homestayPriceDao = database.homestayPriceDao(),
            promotionDao = database.promotionDao()
        )
        val firebaseRepo = FirebaseRepository()

        setContent {
            MaterialTheme {
                val navController = rememberNavController()

                // --- ViewModels ---
                val homeVM: HomeWithDetailsViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            if (modelClass.isAssignableFrom(HomeWithDetailsViewModel::class.java)) {
                                @Suppress("UNCHECKED_CAST")
                                return HomeWithDetailsViewModel(firebaseRepo, homestayRepo, propertyRepo) as T
                            }
                            throw IllegalArgumentException("Unknown ViewModel class")
                        }
                    }
                )

                val propertyVM: PropertyListingViewModel = viewModel(
                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            if (modelClass.isAssignableFrom(PropertyListingViewModel::class.java)) {
                                @Suppress("UNCHECKED_CAST")
                                return PropertyListingViewModel(propertyRepo, SavedStateHandle()) as T
                            }
                            throw IllegalArgumentException("Unknown ViewModel class")
                        }
                    }
                )


                // --- Navigation ---
                NavHost(navController = navController, startDestination = "home") {

                    // --- Home Screen ---
                    composable("home") {
                            HomeScreenWrapper(homeVM = homeVM, navController = navController)
                        }

                    // --- Add Home ---
                    composable("addHome") {
                        HostAddOrEditHomeScreen(
                            propertyVM = propertyVM,
                            homeVM = homeVM,
                            isEdit = false,
                            homeId = null,
                            onBack = { navController.popBackStack() },
                            navController = navController
                        )
                    }

                    // --- Edit Home ---
                    composable(
                        route = "editHome/{homeId}",
                        arguments = listOf(navArgument("homeId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val homeId = backStackEntry.arguments?.getString("homeId")
                        HostAddOrEditHomeScreen(
                            propertyVM = propertyVM,
                            homeVM = homeVM,
                            isEdit = true,
                            homeId = homeId,
                            onBack = { navController.popBackStack() },
                            navController = navController
                        )
                    }

                    composable(
                        route = "addPrice/{homeId}",
                        arguments = listOf(navArgument("homeId") { type = NavType.StringType })
                    ) { backStackEntry ->
                        val homeId = backStackEntry.arguments?.getString("homeId") ?: ""

                        AddPriceScreen(
                            homeId = homeId,           // pass the home ID
                            homeVM = homeVM,           // pass the existing HomeWithDetailsViewModel
                            onBackClick = { navController.popBackStack() },
                            onProfileClick = { /* navigate to profile */ },
                            onSaveClick = { navController.navigate("home") }
                        )
                    }


                    // --- Add Promo Screen ---
                    composable(
                        route = "addPromo/{homeId}/{homeName}",
                        arguments = listOf(
                            navArgument("homeId") { type = NavType.StringType },
                            navArgument("homeName") { type = NavType.StringType }
                        )
                    ) { backStackEntry ->
                        val homeId = backStackEntry.arguments?.getString("homeId") ?: ""
                        val encodedHomeName = backStackEntry.arguments?.getString("homeName") ?: ""
                        val homeName = URLDecoder.decode(encodedHomeName, "UTF-8")
                        AddPromoScreen(
                            homeId = homeId,
                            homeName = homeName,
                            repository = homestayRepo,
                            onBackClick = { navController.popBackStack() },
                            onProfileClick = { /* navigate to profile */ }
                        )
                    }


                }
            }
        }
    }
}
