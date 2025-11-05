package com.grupo2.ashley.profile.models

import com.google.firebase.firestore.PropertyName

data class UserProfile(
    val userId: String = "",
    val email: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val phoneNumber: String = "",
    val address: String = "",
    val city: String = "",
    val postalCode: String = "",
    val profileImageUrl: String = "",
    // Ubicación predeterminada para punto de recogida/entrega
    val defaultPickupLocationName: String = "",
    val defaultPickupLatitude: Double = 0.0,
    val defaultPickupLongitude: Double = 0.0,
    @PropertyName("isProfileComplete")
    val isProfileComplete: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)

data class ProfileUpdateState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)
