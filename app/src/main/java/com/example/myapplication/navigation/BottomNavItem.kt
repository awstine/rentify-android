package com.example.myapplication.navigation

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Apartment
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.myapplication.R

sealed class BottomNavItem(
    val route: String,
    val label: String,
    val activeIcon: Any, // Can be ImageVector or @DrawableRes Int
    val inactiveIcon: Any // Can be ImageVector or @DrawableRes Int
) {
    object Home : BottomNavItem("home", "Home", R.drawable.home, Icons.Outlined.Home)
    object Explore : BottomNavItem("explore", "Rooms", Icons.Outlined.Apartment, Icons.Outlined.Apartment)
    object Rewards : BottomNavItem("rewards", "Rewards", Icons.Filled.Star, Icons.Outlined.StarBorder)
    object Profile : BottomNavItem("profile", "Profile", Icons.Filled.Person, Icons.Outlined.Person)
}