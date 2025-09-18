package com.example.homestay.ui.common

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration

/** True for screens with smallest width >= 600dp (typical tablet breakpoint). */
@Composable
fun isTablet(): Boolean = LocalConfiguration.current.smallestScreenWidthDp >= 600