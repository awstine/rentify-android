package com.example.myapplication.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.example.myapplication.R

val Sans = FontFamily(
    Font(R.font.sansregular, FontWeight.Normal),
    Font(R.font.sansmedium, FontWeight.Medium),
    Font(R.font.sansbold, FontWeight.Bold),
    Font(R.font.sanssemibold, FontWeight.SemiBold),
    Font(R.font.sansitalic, FontWeight.Normal, FontStyle.Italic),
    Font(R.font.sanssemibolditalic, FontWeight.SemiBold, FontStyle.Italic),
)

val GoogleSans = FontFamily(
    Font(R.font.googleansitalic, FontWeight.Normal, FontStyle.Italic)
)

// Set of Material typography styles to start with
val Typography = Typography(
    bodyLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    titleLarge = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = Sans,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)
