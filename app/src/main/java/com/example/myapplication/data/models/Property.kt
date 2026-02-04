package com.example.myapplication.data.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Entity(tableName = "properties")
@Serializable
data class Property(
    @PrimaryKey
    val id: String,
    val name: String,
    val address: String,
    val type: String,
    @SerialName("landlord_id")
    val landlordId: String
)
