package com.grupo2.ashley.profile.models

import com.google.firebase.firestore.Exclude
import com.google.firebase.firestore.PropertyName

data class UserProfile(
    val userId: String = "",
    val email: String = "",
    val firstName: String = "",
    val lastName: String = "",
    val phoneNumber: String = "",
    val profileImageUrl: String = "",
    // Dirección completa de Google Maps
    val fullAddress: String = "",
    // Ubicación predeterminada para punto de recogida/entrega
    val defaultPickupLocationName: String = "",
    val defaultPickupLatitude: Double = 0.0,
    val defaultPickupLongitude: Double = 0.0,
    @get:PropertyName("isProfileComplete")
    val isProfileComplete: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    // Constructor sin argumentos requerido por Firestore
    constructor() : this(
        userId = "",
        email = "",
        firstName = "",
        lastName = "",
        phoneNumber = "",
        profileImageUrl = "",
        fullAddress = "",
        defaultPickupLocationName = "",
        defaultPickupLatitude = 0.0,
        defaultPickupLongitude = 0.0,
        isProfileComplete = false,
        createdAt = System.currentTimeMillis(),
        updatedAt = System.currentTimeMillis()
    )
    
    // Setter para Firestore con compatibilidad para ambos nombres de campo
    @PropertyName("isProfileComplete")
    fun setIsProfileComplete(value: Boolean) = this.copy(isProfileComplete = value)
}

data class ProfileUpdateState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: Boolean = false
)
