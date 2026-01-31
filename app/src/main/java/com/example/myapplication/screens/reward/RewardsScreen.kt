package com.example.myapplication.screens.reward

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myapplication.data.models.Reward
import com.example.myapplication.ui.theme.NavyPrimary
import com.example.myapplication.ui.theme.SuccessGreen


@Composable
fun RewardsScreen(
    viewModel: RewardsViewModel = hiltViewModel()
) {
    val state = viewModel.uiState
    
    val currentStreak = state.streak
    val target = state.target
    val discountAmount = "1,000 KES"
    val progress = if (target > 0) currentStreak.toFloat() / target.toFloat() else 0f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .background(Color.White),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Loyalty Rewards",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = NavyPrimary
        )
        Text(
            text = "Pay rent on time to earn discounts!",
            color = Color.Gray,
            fontSize = 14.sp
        )

        Spacer(modifier = Modifier.height(40.dp))
        
        if (state.isLoading) {
             CircularProgressIndicator(color = NavyPrimary)
        } else if (state.error != null) {
            Text(text = "Error: ${state.error}", color = Color.Red)
        } else {
            // The Progress Card
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F6F8)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Circular Progress or Linear
                    Box(contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            progress = { progress },
                            modifier = Modifier.size(120.dp),
                            color = SuccessGreen,
                            strokeWidth = 10.dp,
                            trackColor = Color.LightGray
                        )
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "$currentStreak / $target",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = NavyPrimary
                            )
                            Text("Months", fontSize = 12.sp, color = Color.Gray)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = "You are on a $currentStreak month streak!",
                        fontWeight = FontWeight.SemiBold,
                        color = NavyPrimary
                    )
                    Text(
                        text = "${target - currentStreak} more months until your discount.",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // The Reward Badge
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color.White, RoundedCornerShape(12.dp))
                            .padding(12.dp)
                    ) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = Color(0xFFFFD700)) // Gold
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "Reward: $discountAmount Off", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(state.rewards) { reward ->
                    RewardCard(reward)
                }
            }
        }
    }
}

@Composable
fun RewardCard(reward: Reward) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF5F6F8)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = reward.icon,
                contentDescription = null,
                tint = NavyPrimary,
                modifier = Modifier.size(40.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = reward.title,
                    fontWeight = FontWeight.Bold,
                    color = NavyPrimary
                )
                Text(
                    text = reward.description,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "${reward.points} pts",
                fontWeight = FontWeight.Bold,
                color = SuccessGreen
            )
        }
    }
}
