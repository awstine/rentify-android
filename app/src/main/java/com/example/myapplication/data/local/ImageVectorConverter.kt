package com.example.myapplication.data.local

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.room.TypeConverter

class ImageVectorConverter {
    @TypeConverter
    fun fromString(value: String?): ImageVector? {
        return if (value == null) null else Icons.Default.Star // A real implementation would map different strings to different icons
    }

    @TypeConverter
    fun toString(imageVector: ImageVector?): String? {
        return if (imageVector == null) null else "star" // A real implementation would map different icons to different strings
    }
}
