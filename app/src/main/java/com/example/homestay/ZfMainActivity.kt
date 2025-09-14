package com.example.homestay

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.homestay.data.local.HomestayDatabase
import com.example.homestay.data.local.UserDatabase
import com.example.homestay.data.repository.FirebaseRepository
import com.example.homestay.data.repository.HomestayRepository
import com.example.homestay.data.repository.PropertyListingRepository
import com.example.homestay.ui.EditProfile.EditProfileScreen
import com.example.homestay.ui.ForgotPassword.ForgotPasswordScreen
import com.example.homestay.ui.HostHome.BookingHistoryScreen
import com.example.homestay.ui.HostHome.BookingRequestsScreen
import com.example.homestay.ui.HostHome.HomeWithDetailsViewModel
import com.example.homestay.ui.HostHome.HomeScreenWrapper
import com.example.homestay.ui.Login.LoginScreen
import com.example.homestay.ui.Logo.LogoScreen
import com.example.homestay.ui.Profile.ProfileScreen
import com.example.homestay.ui.SignUp.SignUpScreen
import com.example.homestay.ui.addPromo.AddPromoScreen
import com.example.homestay.ui.property.HostAddOrEditHomeScreen
import com.example.homestay.ui.property.PropertyListingViewModel
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.net.URLDecoder
import com.example.homestay.ui.client.ClientBrowseScreen

enum class HomeStayScreen {
    Logo,
    Login,
    ForgotPassword,
    SignUp,
    HostHome,
    Profile,
    EditProfile
}

class MainActivity : ComponentActivity() {
    private lateinit var dataStoreManager: DataStoreManager
    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(applicationContext, dataStoreManager)
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        dataStoreManager = DataStoreManager(applicationContext)

        // Check which Firebase project is connected
        val firebaseApp = FirebaseApp.getInstance()
        val projectId = firebaseApp.options.projectId

        Log.d("FIREBASE_CHECK", "🔥 Connected to Firebase Project: $projectId")
        Log.d("FIREBASE_CHECK", "🔥 Expected project: homestayassignment")
        Log.d("FIREBASE_CHECK", "✅ Correct project? ${projectId == "homestayassignment"}")

        try {
            FirebaseApp.initializeApp(this)
            FirebaseAuth.getInstance().addAuthStateListener { auth ->
                val user = auth.currentUser
                if (user != null) {
                    Log.d("FIREBASE_AUTH", "👤 User logged in: ${user.uid}")
                    Log.d("FIREBASE_AUTH", "📧 Email: ${user.email}")
                    Log.d("FIREBASE_AUTH", "🏠 Project: ${FirebaseApp.getInstance().options.projectId}")
                } else {
                    Log.d("FIREBASE_AUTH", "❌ No user logged in")
                }
            }
            Log.d("MainActivity", "Firebase initialized successfully")
        } catch (e: Exception) {
            Log.e("MainActivity", "Firebase initialization failed", e)
        }
        enableEdgeToEdge()

        val userDb = UserDatabase.getDatabase(applicationContext) // your UserDatabase singleton
        lifecycleScope.launch {
            val users = userDb.userDao().getAllUsers()
            Log.d("USER_DB_CHECK", "User DB opened, users count = ${users.size}")
        }

        setContent {
            MaterialTheme {
                Text("Test App Running")
                val windowSizeClass = calculateWindowSizeClass(this)

                //Repositories
                val context = this
                val database = HomestayDatabase.getDatabase(context)
                val dataStoreManager = DataStoreManager(context)
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

                //ViewModels
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

                HomeStayApp(
                    windowSizeClass = windowSizeClass,
                    authViewModel = authViewModel,
                    dataStoreManager = dataStoreManager,
                    homeVM = homeVM,
                    propertyVM = propertyVM,
                    homestayRepo = homestayRepo
                )
            }
        }
    }
}

@Composable
fun HomeStayApp(
    windowSizeClass: WindowSizeClass,
    authViewModel: AuthViewModel,
    dataStoreManager: DataStoreManager,
    homeVM: HomeWithDetailsViewModel,
    propertyVM: PropertyListingViewModel,
    homestayRepo: HomestayRepository
) {
    val navController = rememberNavController()

    val uiState by authViewModel.uiState.collectAsState()
    val isLoggedIn by dataStoreManager.isLoggedIn.collectAsState(initial = false)

    Surface(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = HomeStayScreen.Logo.name) {

            //Home Screen
            composable(HomeStayScreen.HostHome.name) {
                HomeScreenWrapper(homeVM = homeVM, navController = navController)
            }
            composable("bookingRequests") { BookingRequestsScreen(navController) }
            composable("bookingHistory") { BookingHistoryScreen(navController) }

            //Add Home
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

            //Edit Home
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

            //Add Promo Screen
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
                    onProfileClick = { navController.navigate(HomeStayScreen.Profile.name) }
                )
            }

            //Logo Screen
            composable(route = HomeStayScreen.Logo.name) {
                when (windowSizeClass.widthSizeClass) {
                    WindowWidthSizeClass.Compact -> {
                        //Phone layout
                        LogoScreen(
                            isTablet = false,
                            onLoginButtonClicked = {
                                navController.navigate(HomeStayScreen.Login.name)
                            },
                            onSignUpButtonClicked = {
                                navController.navigate(HomeStayScreen.SignUp.name)
                            })
                    }
                    WindowWidthSizeClass.Medium,
                    WindowWidthSizeClass.Expanded -> {
                        //Tablet layout
                        LogoScreen(
                            isTablet = true,
                            onLoginButtonClicked = {
                                navController.navigate(HomeStayScreen.Login.name)
                            },
                            onSignUpButtonClicked = {
                                navController.navigate(HomeStayScreen.SignUp.name)
                            })
                    }
                }
            }

            //Login Screen
            composable(route = HomeStayScreen.Login.name) {
                when (windowSizeClass.widthSizeClass) {
                    WindowWidthSizeClass.Compact -> {
                        //Phone layout
                        LoginScreen(
                            isTablet = false,
                            viewModel = authViewModel,
                            onSuccess = { navController.navigate(HomeStayScreen.HostHome.name) },
                            onBackButtonClicked = { navController.navigate(HomeStayScreen.Logo.name) },
                            onForgotPasswordClicked = { navController.navigate(HomeStayScreen.ForgotPassword.name) })
                    }
                    WindowWidthSizeClass.Medium,
                    WindowWidthSizeClass.Expanded -> {
                        //Tablet layout
                        LoginScreen(
                            isTablet = true,
                            viewModel = authViewModel,
                            onSuccess = { navController.navigate(HomeStayScreen.HostHome.name) },
                            onBackButtonClicked = { navController.navigate(HomeStayScreen.Logo.name) },
                            onForgotPasswordClicked = { navController.navigate(HomeStayScreen.ForgotPassword.name) })
                    }
                }
            }

            //Forgot Password
            composable(route = HomeStayScreen.ForgotPassword.name) {
                when (windowSizeClass.widthSizeClass) {
                    WindowWidthSizeClass.Compact -> {
                        //Phone layout
                        ForgotPasswordScreen(
                            isTablet = false,
                            viewModel = authViewModel,
                            onBackButtonClicked = { navController.popBackStack() })
                    }
                    WindowWidthSizeClass.Medium,
                    WindowWidthSizeClass.Expanded -> {
                        //Tablet layout
                        ForgotPasswordScreen(
                            isTablet = true,
                            viewModel = authViewModel,
                            onBackButtonClicked = { navController.popBackStack() })
                    }
                }
            }

            //Sign Up
            composable(route = HomeStayScreen.SignUp.name) {
                when (windowSizeClass.widthSizeClass) {
                    WindowWidthSizeClass.Compact -> {
                        //Phone layout
                        SignUpScreen(
                            isTablet = false,
                            viewModel = authViewModel,
                            onBackButtonClicked = { navController.popBackStack() },
                            onSuccess = { navController.navigate(HomeStayScreen.Logo.name) })
                    }
                    WindowWidthSizeClass.Medium,
                    WindowWidthSizeClass.Expanded -> {
                        //Tablet layout
                        SignUpScreen(
                            isTablet = true,
                            viewModel = authViewModel,
                            onBackButtonClicked = { navController.popBackStack() },
                            onSuccess = { navController.navigate(HomeStayScreen.Logo.name) })
                    }
                }
            }

            //user screen
            composable("clientBrowse") {
                ClientBrowseScreen(
                    vm = propertyVM,
                    onProfileClick = { navController.navigate("clientProfile") },
                    onBottomHome = { navController.navigate("clientBrowse") },
                    onBottomExplore = { /* current */ },
                    onBottomSettings = { navController.navigate("clientSettings") }
                )
            }

            //Profile
            composable(route = HomeStayScreen.Profile.name) {
                when (windowSizeClass.widthSizeClass) {
                    WindowWidthSizeClass.Compact -> {
                        //Phone layout
                        ProfileScreen(
                            isTablet = false,
                            viewModel = authViewModel,
                            onBackButtonClicked = { navController.navigate(HomeStayScreen.HostHome.name) },
                            onEditProfileClicked = { navController.navigate(HomeStayScreen.EditProfile.name) },
                            onLogoutClicked = { authViewModel.logout()},
                            onDeleteAccountClicked = { navController.navigate(HomeStayScreen.Logo.name)})
                    }
                    WindowWidthSizeClass.Medium,
                    WindowWidthSizeClass.Expanded -> {
                        //Tablet layout
                        ProfileScreen(
                            isTablet = true,
                            viewModel = authViewModel,
                            onBackButtonClicked = { navController.navigate(HomeStayScreen.HostHome.name) },
                            onEditProfileClicked = { navController.navigate(HomeStayScreen.EditProfile.name) },
                            onLogoutClicked = { authViewModel.logout()},
                            onDeleteAccountClicked = { navController.navigate(HomeStayScreen.Logo.name)})
                    }
                }
            }
            composable("clientBrowse") {
                ClientBrowseScreen(
                    vm = propertyVM,
                    onProfileClick = { navController.navigate("clientProfile") },
                    onBottomHome = { navController.navigate("clientBrowse") },
                    onBottomExplore = { /* current */ },
                    onBottomSettings = { navController.navigate("clientSettings") }
                )
            }
            //Edit Profile
            composable(route = HomeStayScreen.EditProfile.name) {
                when (windowSizeClass.widthSizeClass) {
                    WindowWidthSizeClass.Compact -> {
                        //Phone layout
                        EditProfileScreen(
                            isTablet = false,
                            viewModel = authViewModel,
                            onBackButtonClicked = { navController.popBackStack() },
                            onSaveSuccess = { navController.navigate(HomeStayScreen.Profile.name) })
                    }
                    WindowWidthSizeClass.Medium,
                    WindowWidthSizeClass.Expanded -> {
                        //Tablet layout
                        EditProfileScreen(
                            isTablet = true,
                            viewModel = authViewModel,
                            onBackButtonClicked = { navController.popBackStack() },
                            onSaveSuccess = { navController.navigate(HomeStayScreen.Profile.name) })
                    }
                }
            }
        }

        LaunchedEffect(isLoggedIn, uiState.userProfile) {
            if (isLoggedIn && uiState.userProfile != null) {
                navController.navigate(HomeStayScreen.HostHome.name) {
                    popUpTo(0) { inclusive = true } // clear backstack
                }
            } else if (!isLoggedIn) {
                navController.navigate(HomeStayScreen.Logo.name) {
                    popUpTo(0) { inclusive = true } // clear backstack
                }
            }
        }
    }
}