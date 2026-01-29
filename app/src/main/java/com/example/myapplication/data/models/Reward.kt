package com.example.myapplication.data.models

import androidx.compose.ui.graphics.vector.ImageVector

data class Reward(
    val title: String,
    val description: String,
    val points: Int,
    val icon: ImageVector
)