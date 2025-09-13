//package com.example.homestay
//
//import android.os.Bundle
//import android.util.Log
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.setContent
//import androidx.activity.enableEdgeToEdge
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.material3.Surface
//import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
//import androidx.compose.material3.windowsizeclass.WindowSizeClass
//import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
//import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Modifier
//import androidx.navigation.compose.NavHost
//import androidx.navigation.compose.composable
//import androidx.navigation.compose.rememberNavController
//import com.example.homestay.ui.EditProfile.EditProfileScreen
//import com.example.homestay.ui.ForgotPassword.ForgotPasswordScreen
//import com.example.homestay.ui.Login.LoginScreen
//import com.example.homestay.ui.Logo.LogoScreen
//import com.example.homestay.ui.SignUp.SignUpScreen
//import com.google.firebase.FirebaseApp
//import androidx.activity.viewModels
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Text
//import androidx.compose.runtime.collectAsState
//import com.example.homestay.ui.Profile.ProfileScreen
//
//class MainActivity : ComponentActivity() {
//    private val authViewModel: AuthViewModel by viewModels {
//        AuthViewModelFactory(applicationContext)
//    }
//
//    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        try {
//            FirebaseApp.initializeApp(this)
//            Log.d("MainActivity", "Firebase initialized successfully")
//        } catch (e: Exception) {
//            Log.e("MainActivity", "Firebase initialization failed", e)
//        }
//        enableEdgeToEdge()
//        setContent {
//            MaterialTheme {
//                Text("Test App Running")
//                val windowSizeClass = calculateWindowSizeClass(this)
//                HomeStayApp(windowSizeClass, authViewModel)
//            }
//        }
//    }
//}
//
//enum class HomeStayScreen{
//    Logo,
//    Login,
//    ForgotPassword,
//    SignUp,
//    Home,
//    Profile,
//    EditProfile
//}
//
//@Composable
//fun HomeStayApp(windowSizeClass: WindowSizeClass, authViewModel: AuthViewModel) {
//    val navController = rememberNavController()
//    var selectedRole by remember { mutableStateOf("") }
//
//    val uiState by authViewModel.uiState.collectAsState()
//
//    val startDestination = if (uiState.isLoggedIn) {
//        HomeStayScreen.Home.name  //directly enter Home page
//    } else {
//        HomeStayScreen.Logo.name
//    }
//
//    Surface(modifier = Modifier.fillMaxSize()) {
//        NavHost(navController = navController,
//            startDestination = startDestination){
//
//            composable(route = HomeStayScreen.Logo.name){
//                when(windowSizeClass.widthSizeClass){
//                    WindowWidthSizeClass.Compact -> {
//                        //Phone layout
//                        LogoScreen(isTablet = false,
//                            selectedRole = selectedRole,
//                            onRoleChange = { selectedRole = it },
//                            onLoginButtonClicked = {
//                                navController.navigate(HomeStayScreen.Login.name)
//                            },
//                            onSignUpButtonClicked = {
//                                navController.navigate(HomeStayScreen.SignUp.name)
//                            })
//                    }
//                    WindowWidthSizeClass.Medium,
//                    WindowWidthSizeClass.Expanded -> {
//                        //Tablet layout
//                        LogoScreen(isTablet = true,
//                            selectedRole = selectedRole,
//                            onRoleChange = { selectedRole = it },
//                            onLoginButtonClicked = {
//                                navController.navigate(HomeStayScreen.Login.name)
//                            },
//                            onSignUpButtonClicked = {
//                                navController.navigate(HomeStayScreen.SignUp.name)
//                            })
//                    }
//                }
//            }
//
//            composable(route = HomeStayScreen.Login.name){
//                when(windowSizeClass.widthSizeClass){
//                    WindowWidthSizeClass.Compact -> {
//                        //Phone layout
//                        LoginScreen(isTablet = false,
//                            viewModel = authViewModel,
//                            onSuccess = { navController.navigate(HomeStayScreen.Home.name)},
//                            onBackButtonClicked = { navController.navigate(HomeStayScreen.Logo.name)},
//                            onForgotPasswordClicked = { navController.navigate(HomeStayScreen.ForgotPassword.name)})
//                    }
//                    WindowWidthSizeClass.Medium,
//                    WindowWidthSizeClass.Expanded -> {
//                        //Tablet layout
//                        LoginScreen(isTablet = true,
//                            viewModel = authViewModel,
//                            onSuccess = { navController.navigate(HomeStayScreen.Home.name)},
//                            onBackButtonClicked = { navController.navigate(HomeStayScreen.Logo.name)},
//                            onForgotPasswordClicked = { navController.navigate(HomeStayScreen.ForgotPassword.name)})
//                    }
//                }
//            }
//
//            composable(route = HomeStayScreen.ForgotPassword.name){
//                when(windowSizeClass.widthSizeClass){
//                    WindowWidthSizeClass.Compact -> {
//                        //Phone layout
//                        ForgotPasswordScreen(isTablet = false,
//                            viewModel = authViewModel,
//                            onBackButtonClicked = { navController.popBackStack() })
//                    }
//                    WindowWidthSizeClass.Medium,
//                    WindowWidthSizeClass.Expanded -> {
//                        //Tablet layout
//                        ForgotPasswordScreen(isTablet = true,
//                            viewModel = authViewModel,
//                            onBackButtonClicked = { navController.popBackStack() })
//                    }
//                }
//            }
//
//            composable(route = HomeStayScreen.SignUp.name){
//                when(windowSizeClass.widthSizeClass){
//                    WindowWidthSizeClass.Compact -> {
//                        //Phone layout
//                        SignUpScreen(isTablet = false,
//                            viewModel = authViewModel,
//                            onBackButtonClicked = { navController.popBackStack()},
//                            onSuccess = { navController.popBackStack()})
//                    }
//                    WindowWidthSizeClass.Medium,
//                    WindowWidthSizeClass.Expanded -> {
//                        //Tablet layout
//                        SignUpScreen(isTablet = true,
//                            viewModel = authViewModel,
//                            onBackButtonClicked = { navController.popBackStack()},
//                            onSuccess = { navController.popBackStack()})
//                    }
//                }
//            }
//
//            composable(route = HomeStayScreen.Profile.name){
//                when(windowSizeClass.widthSizeClass){
//                    WindowWidthSizeClass.Compact -> {
//                        //Phone layout
//                        ProfileScreen(isTablet = false,
//                            viewModel = authViewModel,
//                            onBackButtonClicked = { navController.navigate("Home")},
//                            onEditProfileClicked = { navController.navigate(HomeStayScreen.EditProfile.name)})
//                    }
//                    WindowWidthSizeClass.Medium,
//                    WindowWidthSizeClass.Expanded -> {
//                        //Tablet layout
//                        ProfileScreen(isTablet = true,
//                            viewModel = authViewModel,
//                            onBackButtonClicked = { navController.navigate(HomeStayScreen.Home.name)},
//                            onEditProfileClicked = { navController.navigate(HomeStayScreen.EditProfile.name)})
//                    }
//                }
//            }
//
//            composable(route = HomeStayScreen.EditProfile.name){
//                when(windowSizeClass.widthSizeClass){
//                    WindowWidthSizeClass.Compact -> {
//                        //Phone layout
//                        EditProfileScreen(isTablet = false,
//                            viewModel = authViewModel,
//                            onBackButtonClicked = { navController.popBackStack() },
//                            onSaveSuccess = { navController.popBackStack() })
//                    }
//                    WindowWidthSizeClass.Medium,
//                    WindowWidthSizeClass.Expanded -> {
//                        //Tablet layout
//                        EditProfileScreen(isTablet = true,
//                            viewModel = authViewModel,
//                            onBackButtonClicked = { navController.popBackStack() },
//                            onSaveSuccess = { navController.popBackStack() })
//                    }
//                }
//            }
//        }
//    }
//}