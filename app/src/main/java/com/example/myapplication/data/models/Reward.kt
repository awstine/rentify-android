package com.example.myapplication.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.myapplication.data.local.ImageVectorConverter
import androidx.compose.ui.graphics.vector.ImageVector

@Entity(tableName = "rewards")
@TypeConverters(ImageVectorConverter::class)
data class Reward(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String,
    val points: Int,
    val icon: ImageVector
)
