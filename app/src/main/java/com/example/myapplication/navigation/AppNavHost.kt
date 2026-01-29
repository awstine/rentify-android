package com.example.myapplication.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.myapplication.screens.LoadingScreen
import com.example.myapplication.screens.MainAppScreen
import com.example.myapplication.screens.auth.login.LoginScreen
import com.example.myapplication.screens.auth.register.RegisterScreen
import com.example.myapplication.screens.auth.reset_password.ResetPasswordScreen
import com.example.myapplication.screens.history.HistoryScreen

@Composable
fun AppNavHost() {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = Screen.Loading
    ) {
        composable<Screen.Loading> {
            LoadingScreen(
                onNavigateToHome = {
                    navController.navigate(Screen.MainScreen) {
                        popUpTo(Screen.Loading) { inclusive = true }
                    }
                },
                onNavigateToLogin = {
                    navController.navigate(Screen.Login) {
                        popUpTo(Screen.Loading) { inclusive = true }
                    }
                }
            )
        }
        composable<Screen.Login> {
            LoginScreen(
                onLoginSuccess = {
                    navController.navigate(Screen.MainScreen) {
                        popUpTo(Screen.Login) { inclusive = true }
                    }
                },
                onForgotPassword = {
                    navController.navigate(Screen.Reset){
                        // popUpTo(Screen.Login) { inclusive = true }
                    }
                } ,
                onNavigateToRegister = {
                    navController.navigate(Screen.Register) {
                       // popUpTo(Screen.Login) { inclusive = true }
                    }
                }
            )
        }
        composable<Screen.Register>{
            RegisterScreen(
                onRegisterSuccess = {
                    navController.navigate(Screen.Login)
                }
            )
        }
        composable<Screen.Reset> {
            ResetPasswordScreen(
                onPasswordReset = {}
            )
        }
        composable<Screen.MainScreen> {
            MainAppScreen(
                onSignOut = {
                    navController.navigate(Screen.Login) {
                        popUpTo(Screen.MainScreen) { inclusive = true }
                    }
                },
                onNavigateToPaymentHistory = {
                    navController.navigate(Screen.PaymentHistory)
                }
            )
        }
        composable<Screen.PaymentHistory>{
                HistoryScreen(
                    onBack = { navController.popBackStack() }
                )
        }
    }
}
