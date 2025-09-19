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
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
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
import com.example.homestay.ui.HostHome.HomeWithDetailsViewModel
import com.example.homestay.ui.HostHome.HomeScreenWrapper
import com.example.homestay.ui.Login.LoginScreen
import com.example.homestay.ui.Logo.LogoScreen
import com.example.homestay.ui.Profile.ProfileScreen
import com.example.homestay.ui.SignUp.SignUpScreen
import com.example.homestay.ui.addPromo.AddPromoScreen
import com.example.homestay.ui.booking.BookingViewModel
import com.example.homestay.ui.property.HostAddOrEditHomeScreen
import com.example.homestay.ui.property.PropertyListingViewModel
import com.example.homestay.ui.client.ClientBrowseScreen
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.launch
import java.net.URLDecoder
import com.google.firebase.firestore.FirebaseFirestore
import com.example.homestay.data.repository.FirestoreBookingRepository
import com.example.homestay.ui.HostHome.BookingRequestsScreen
import com.example.homestay.ui.booking.UpdateBookingScreen
import java.util.Date

// ✅ Added ClientBrowse
enum class HomeStayScreen {
    Logo,
    Login,
    ForgotPassword,
    SignUp,
    HostHome,
    Profile,
    EditProfile,
    ClientBrowse  // 👈 everything booking-related inside here
}



class MainActivity : ComponentActivity() {
    private val authViewModel: AuthViewModel by viewModels {
        AuthViewModelFactory(applicationContext, DataStoreManager(applicationContext))
    }

    val uid = FirebaseAuth.getInstance().currentUser?.uid.orEmpty()


    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)

        // Firebase project check
        val firebaseApp = FirebaseApp.getInstance()
        val projectId = firebaseApp.options.projectId
        Log.d("FIREBASE_CHECK", "🔥 Connected to Firebase Project: $projectId")

        try {
            FirebaseAuth.getInstance().addAuthStateListener { auth ->
                val user = auth.currentUser
                if (user != null) {
                    Log.d("FIREBASE_AUTH", "👤 User logged in: ${user.uid}")
                    Log.d("FIREBASE_AUTH", "📧 Email: ${user.email}")
                } else {
                    Log.d("FIREBASE_AUTH", "❌ No user logged in")
                }
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Firebase initialization failed", e)
        }

        enableEdgeToEdge()

        val userDb = UserDatabase.getDatabase(applicationContext)
        lifecycleScope.launch {
            val users = userDb.userDao().getAllUsers()
            Log.d("USER_DB_CHECK", "User DB opened, users count = ${users.size}")
        }

        setContent {
            MaterialTheme {
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
                    factory = object : ViewModelProvider.Factory {
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
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            if (modelClass.isAssignableFrom(PropertyListingViewModel::class.java)) {
                                @Suppress("UNCHECKED_CAST")
                                return PropertyListingViewModel(propertyRepo, SavedStateHandle()) as T
                            }
                            throw IllegalArgumentException("Unknown ViewModel class")
                        }
                    }
                )

                val bookingRepo = FirestoreBookingRepository(firestore = FirebaseFirestore.getInstance())

                val bookingVM: BookingViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            if (modelClass.isAssignableFrom(BookingViewModel::class.java)) {
                                @Suppress("UNCHECKED_CAST")
                                return BookingViewModel(bookingRepo) as T
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
                    homestayRepo = homestayRepo,
                    bookingVM = bookingVM,
                    propertyRepo = propertyRepo

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
    homestayRepo: HomestayRepository,
    bookingVM: BookingViewModel,
    propertyRepo: PropertyListingRepository
) {
    val navController = rememberNavController()
    val uiState by authViewModel.uiState.collectAsState()
    val isLoggedIn by dataStoreManager.isLoggedIn.collectAsState(initial = false)
    val userRole by dataStoreManager.userRole.collectAsState(initial = null)

    LaunchedEffect(Unit) {
        authViewModel.checkAuthStatus()
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        NavHost(navController = navController, startDestination = HomeStayScreen.Logo.name) {

            // Host Home
            composable(HomeStayScreen.HostHome.name) {
                HomeScreenWrapper(homeVM = homeVM, navController = navController)
            }
            composable(
                route = "bookingRequests/{hostId}"
            ) { backStackEntry ->
                val hostId = backStackEntry.arguments?.getString("hostId") ?: return@composable
                bookingVM.setCurrentHost(hostId)  // load bookings for this host
                BookingRequestsScreen(navController = navController, bookingVM = bookingVM, propertyVM = propertyVM)
            }

            composable("bookingHistory") {
                BookingHistoryScreen(
                    navController = navController,
                    bookingVM = bookingVM,
                    homeRepo = propertyRepo // 👈 use repository directly
                )
            }

            // Add Home
            composable("addHome") {
                HostAddOrEditHomeScreen(
                    propertyVM = propertyVM,
                    homeVM = homeVM,
                    isEdit = false,
                    homeId = null,
                    authViewModel = authViewModel,
                    onBack = { navController.popBackStack() },
                    navController = navController
                )
            }

            // Edit Home
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
                    authViewModel = authViewModel,
                    onBack = { navController.popBackStack() },
                    navController = navController
                )
            }

            // Add Promo
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

            // Logo
            composable(HomeStayScreen.Logo.name) {
                val isTablet = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
                LogoScreen(
                    isTablet = isTablet,
                    onLoginButtonClicked = { navController.navigate(HomeStayScreen.Login.name) },
                    onSignUpButtonClicked = { navController.navigate(HomeStayScreen.SignUp.name) }
                )
            }

            // Login
            composable(HomeStayScreen.Login.name) {
                val isTablet = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
                LoginScreen(
                    isTablet = isTablet,
                    viewModel = authViewModel,
                    onSuccess = { Log.d("LOGIN", "Login success, waiting for global navigation") },
                    onBackButtonClicked = { navController.navigate(HomeStayScreen.Logo.name) },
                    onForgotPasswordClicked = { navController.navigate(HomeStayScreen.ForgotPassword.name) }
                )
            }

            // Forgot Password
            composable(HomeStayScreen.ForgotPassword.name) {
                val isTablet = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
                ForgotPasswordScreen(
                    isTablet = isTablet,
                    viewModel = authViewModel,
                    onBackButtonClicked = { navController.popBackStack() }
                )
            }

            // Sign Up
            composable(HomeStayScreen.SignUp.name) {
                val isTablet = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
                SignUpScreen(
                    isTablet = isTablet,
                    viewModel = authViewModel,
                    onBackButtonClicked = { navController.popBackStack() },
                    onSuccess = { navController.navigate(HomeStayScreen.Logo.name) }
                )
            }

            // ✅ Client Browse
            composable(HomeStayScreen.ClientBrowse.name) {
                ClientBrowseScreen(
                    vm = propertyVM,
                    bookingVm = bookingVM,
                    navController = navController,
                    onBottomHome = { navController.navigate(HomeStayScreen.ClientBrowse.name) },
                    onBottomExplore = { /* current */ },
                    onBottomProfile = { navController.navigate(HomeStayScreen.Profile.name) }
                )
            }
            composable("updateBooking") {
                val savedStateHandle = navController.previousBackStackEntry?.savedStateHandle
                val bookingId = savedStateHandle?.get<String>("bookingId") ?: ""
                val currentGuests = savedStateHandle?.get<Int>("currentGuests") ?: 1
                val currentNights = savedStateHandle?.get<Int>("currentNights") ?: 1
                val currentCheckInMillis =
                    savedStateHandle?.get<Long>("currentCheckIn") ?: System.currentTimeMillis()
                val currentCheckInDate = Date(currentCheckInMillis)

                UpdateBookingScreen(
                    bookingVm = bookingVM,
                    bookingId = bookingId,
                    currentGuests = currentGuests,
                    currentNights = currentNights,
                    currentCheckIn = currentCheckInDate,
                    navController = navController
                )
            }


            // Profile
            composable(HomeStayScreen.Profile.name) {
                val isTablet = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
                ProfileScreen(
                    isTablet = isTablet,
                    viewModel = authViewModel,
                    onBackButtonClicked = { navController.popBackStack() },
                    onEditProfileClicked = { navController.navigate(HomeStayScreen.EditProfile.name) },
                    onLogoutClicked = { authViewModel.logout()
                        navController.navigate(HomeStayScreen.Logo.name)},
                    onDeleteAccountClicked = { navController.navigate(HomeStayScreen.Logo.name) }
                )
            }

            // Edit Profile
            composable(HomeStayScreen.EditProfile.name) {
                val isTablet = windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact
                EditProfileScreen(
                    isTablet = isTablet,
                    viewModel = authViewModel,
                    onBackButtonClicked = { navController.popBackStack() },
                    onSaveSuccess = { navController.popBackStack() }
                )
            }
        }

        var hasNavigated by remember { mutableStateOf(false) }

        LaunchedEffect(isLoggedIn, userRole) {
            if (isLoggedIn && userRole != null && !hasNavigated) {
                hasNavigated = true
                when (userRole) {
                    "Host" -> {
                        homeVM.setHostId(uiState.userProfile?.userId ?: "")
                        propertyVM.setHostId(uiState.userProfile?.userId ?: "")
                        navController.navigate(HomeStayScreen.HostHome.name) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                    }

                    "Guest" -> {
                        val userId = uiState.userProfile?.userId ?: ""
                        bookingVM.setCurrentUser(userId)
                        bookingVM.loadUserBookings(userId)
                        navController.navigate(HomeStayScreen.ClientBrowse.name) {
                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                        }
                    }
                }
            }

            if (!isLoggedIn) {
                hasNavigated = false
                navController.navigate(HomeStayScreen.Logo.name) {
                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                }
            }
        }
    }
}