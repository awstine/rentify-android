package com.example.myapplication.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.myapplication.navigation.BottomNavItem
import com.example.myapplication.screens.admin.AdminHomeScreen
import com.example.myapplication.screens.auth.login.LoginScreen
import com.example.myapplication.screens.payment.PaymentScreen
import com.example.myapplication.screens.profile.ProfileScreen
import com.example.myapplication.screens.reward.RewardsScreen
import com.example.myapplication.screens.room.RoomListScreen
import com.example.myapplication.screens.tenant.TenantHomeScreen

@Composable
fun MainAppScreen(
    onSignOut: () -> Unit,
    onNavigateToPaymentHistory: () -> Unit,
    viewModel: MainViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    val userRole = viewModel.userRole

    if (viewModel.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else if (userRole == null) {
        LoginScreen(
            onLoginSuccess = { role ->
                viewModel.onLoginSuccess(role)
            },
            onForgotPassword = { /* TODO */ },
            onNavigateToRegister = { /* TODO */ }
        )
    } else {
        val navItems = remember(userRole) {
            if (userRole == "admin") {
                listOf(BottomNavItem.Home, BottomNavItem.Explore, BottomNavItem.Profile)
            } else {
                listOf(BottomNavItem.Home, BottomNavItem.Explore, BottomNavItem.Rewards, BottomNavItem.Profile)
            }
        }

        Scaffold(
            bottomBar = { StyledBottomBar(navController = navController, items = navItems) }
        ) { innerPadding ->
            NavHost(
                navController = navController,
                startDestination = BottomNavItem.Home.route,
                modifier = Modifier.padding(innerPadding)
            ) {
                composable(BottomNavItem.Home.route) {
                    if (userRole == "admin") {
                        AdminHomeScreen()
                    } else {
                        TenantHomeScreen(
                            onNavigateToPayment = { bookingId, amount, roomNumber ->
                                navController.navigate("payment/$bookingId/$amount/$roomNumber")
                            },
                            onNavigateToPaymentHistory = onNavigateToPaymentHistory
                        )
                    }
                }
                composable(BottomNavItem.Explore.route) {
                    RoomListScreen(onNavigateToPayment = { bookingId, amount, roomNumber ->
                        navController.navigate("payment/$bookingId/$amount/$roomNumber")
                    })
                }
                composable(BottomNavItem.Rewards.route) {
                    if (userRole != "admin") {
                        RewardsScreen()
                    }
                }
                composable(BottomNavItem.Profile.route) {
                    ProfileScreen(onSignOut = {
                        viewModel.onSignOut()
                        onSignOut()
                    })
                }

                composable(
                    route = "payment/{bookingId}/{amount}/{roomNumber}",
                    arguments = listOf(
                        navArgument("bookingId") { type = NavType.StringType },
                        navArgument("amount") { type = NavType.FloatType },
                        navArgument("roomNumber") { type = NavType.StringType }
                    )
                ) { backStackEntry ->
                    val bookingId = backStackEntry.arguments?.getString("bookingId") ?: ""
                    val amount = backStackEntry.arguments?.getFloat("amount")?.toDouble() ?: 0.0
                    val roomNumber = backStackEntry.arguments?.getString("roomNumber") ?: ""

                    PaymentScreen(
                        bookingId = bookingId,
                        amount = amount,
                        roomNumber = roomNumber,
                        onPaymentSuccess = { navController.popBackStack() },
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}

@Composable
fun StyledBottomBar(navController: NavHostController, items: List<BottomNavItem>) {
    var selectedRoute by remember { mutableStateOf(BottomNavItem.Home.route) }

    Surface(
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 0.dp // Remove default tint
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 25.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            items.forEach { item ->
                val isSelected = selectedRoute == item.route
                StyledNavigationItem(item = item, isSelected = isSelected) {
                    selectedRoute = item.route
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            }
        }
    }
}

@Composable
fun RowScope.StyledNavigationItem(
    item: BottomNavItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(35.dp))
                .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            val icon = if (isSelected || item.route == BottomNavItem.Home.route) item.activeIcon else item.inactiveIcon
            val tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.primary

            when (icon) {
                is ImageVector -> Icon(
                    imageVector = icon,
                    contentDescription = item.label,
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
                is Int -> Icon(
                    painter = painterResource(id = icon),
                    contentDescription = item.label,
                    tint = tint,
                    modifier = Modifier.size(24.dp)
                )
            }

            AnimatedVisibility(visible = isSelected) {
                Row {
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = item.label,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Thin,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }
}