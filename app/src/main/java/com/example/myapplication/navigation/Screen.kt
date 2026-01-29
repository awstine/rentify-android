package com.example.myapplication.navigation

import kotlinx.serialization.Serializable

// A sealed interface is a great way to group all your destinations
sealed interface Screen {

    @Serializable
    data object Loading : Screen

    @Serializable
    data object Register : Screen

    @Serializable
    data object Login : Screen

    @Serializable
    data object Reset : Screen

    @Serializable
    data object Home : Screen

    @Serializable
    data object Profile : Screen

    @Serializable
    data object Tenant : Screen

    @Serializable
    data object Rewards : Screen

    @Serializable
    data object Room : Screen
    
    @Serializable
    data object PaymentHistory : Screen

    @Serializable
    data object MainScreen : Screen
}
