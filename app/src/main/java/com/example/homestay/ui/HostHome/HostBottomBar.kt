package com.example.homestay.ui.HostHome

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.runtime.getValue
import com.example.homestay.HomeStayScreen
import com.google.firebase.auth.FirebaseAuth


@Composable
fun HostBottomBar(navController: NavController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    NavigationBar(containerColor = Color.White) {
        NavigationBarItem(
            selected = currentRoute?.startsWith("bookingRequests") == true,
            onClick = {
                if (currentRoute?.startsWith("bookingRequests") != true) {
                    val hostId = FirebaseAuth.getInstance().currentUser?.uid ?: return@NavigationBarItem
                    navController.navigate("bookingRequests/$hostId") {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }

                }
            },
            icon = { Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Requests") },
            label = { Text("Booking Requests") }
        )

        NavigationBarItem(
            selected = currentRoute == HomeStayScreen.HostHome.name,
            onClick = {
                if (currentRoute != HomeStayScreen.HostHome.name) {
                    navController.navigate(HomeStayScreen.HostHome.name) {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
            icon = { Icon(Icons.Default.Place, contentDescription = "Home") },
            label = { Text("Home") }
        )
        NavigationBarItem(
            selected = currentRoute == "bookingHistory",
            onClick = {
                if (currentRoute != "bookingHistory") {
                    navController.navigate("bookingHistory") {
                        popUpTo(navController.graph.startDestinationId) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            },
            icon = { Icon(Icons.Default.History, contentDescription = "History") },
            label = { Text("Booking History") }
        )
    }
}