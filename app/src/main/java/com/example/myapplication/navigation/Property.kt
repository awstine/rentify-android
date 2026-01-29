package com.example.myapplication.navigation

// --- 1. Data Model ---
data class Property(
    val id: Int,
    val imageUrl: String, // In a real app, this is a URL. Here we use placeholders.
    val dateStatus: String,
    val price: String,
    val address: String,
    val baths: Int,
    val beds: Int,
    val sqft: Int
)
