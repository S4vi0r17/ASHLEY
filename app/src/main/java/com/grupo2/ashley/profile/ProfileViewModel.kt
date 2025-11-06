package com.grupo2.ashley.profile

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo2.ashley.profile.data.ImageStorageRepository
import com.grupo2.ashley.profile.data.ProfileRepository
import com.grupo2.ashley.profile.models.ProfileUpdateState
import com.grupo2.ashley.profile.models.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {
    private val repository = ProfileRepository()
    private val imageRepository = ImageStorageRepository()

    private val _userProfile = MutableStateFlow<UserProfile?>(null)
    val userProfile: StateFlow<UserProfile?> = _userProfile.asStateFlow()

    private val _updateState = MutableStateFlow(ProfileUpdateState())
    val updateState: StateFlow<ProfileUpdateState> = _updateState.asStateFlow()

    private val _isProfileComplete = MutableStateFlow(false)
    val isProfileComplete: StateFlow<Boolean> = _isProfileComplete.asStateFlow()

    // Campos del formulario
    private val _firstName = MutableStateFlow("")
    val firstName: StateFlow<String> = _firstName.asStateFlow()

    private val _lastName = MutableStateFlow("")
    val lastName: StateFlow<String> = _lastName.asStateFlow()

    private val _phoneNumber = MutableStateFlow("")
    val phoneNumber: StateFlow<String> = _phoneNumber.asStateFlow()

    private val _fullAddress = MutableStateFlow("")
    val fullAddress: StateFlow<String> = _fullAddress.asStateFlow()

    private val _profileImageUrl = MutableStateFlow("")
    val profileImageUrl: StateFlow<String> = _profileImageUrl.asStateFlow()

    private val _isUploadingImage = MutableStateFlow(false)
    val isUploadingImage: StateFlow<Boolean> = _isUploadingImage.asStateFlow()

    // Ubicación predeterminada
    private val _defaultPickupLocationName = MutableStateFlow("")
    val defaultPickupLocationName: StateFlow<String> = _defaultPickupLocationName.asStateFlow()

    private val _defaultPickupLatitude = MutableStateFlow(0.0)
    val defaultPickupLatitude: StateFlow<Double> = _defaultPickupLatitude.asStateFlow()

    private val _defaultPickupLongitude = MutableStateFlow(0.0)
    val defaultPickupLongitude: StateFlow<Double> = _defaultPickupLongitude.asStateFlow()

    companion object {
        private const val TAG = "ProfileViewModel"
    }

    init {
        loadUserProfile()
        loadGooglePhotoIfAvailable()
    }

    /**
     * Carga el perfil del usuario desde Firestore
     */
    fun loadUserProfile() {
        viewModelScope.launch {
            _updateState.value = ProfileUpdateState(isLoading = true)
            
            repository.getUserProfile().fold(
                onSuccess = { profile ->
                    _userProfile.value = profile
                    profile?.let {
                        _firstName.value = it.firstName
                        _lastName.value = it.lastName
                        _phoneNumber.value = it.phoneNumber
                        _fullAddress.value = it.fullAddress
                        _profileImageUrl.value = it.profileImageUrl
                        _defaultPickupLocationName.value = it.defaultPickupLocationName
                        _defaultPickupLatitude.value = it.defaultPickupLatitude
                        _defaultPickupLongitude.value = it.defaultPickupLongitude
                        _isProfileComplete.value = it.isProfileComplete
                    }
                    _updateState.value = ProfileUpdateState(isLoading = false)
                    Log.d(TAG, "Perfil cargado exitosamente")
                },
                onFailure = { error ->
                    _updateState.value = ProfileUpdateState(
                        isLoading = false,
                        error = "Error al cargar perfil: ${error.message}"
                    )
                    Log.e(TAG, "Error al cargar perfil", error)
                }
            )
        }
    }

    /**
     * Carga la foto de Google si está disponible y el usuario no tiene foto
     */
    private fun loadGooglePhotoIfAvailable() {
        viewModelScope.launch {
            try {
                val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
                val currentUser = auth.currentUser
                val googlePhotoUrl = currentUser?.photoUrl?.toString()
                
                if (!googlePhotoUrl.isNullOrBlank() && _profileImageUrl.value.isBlank()) {
                    _profileImageUrl.value = googlePhotoUrl
                    Log.d(TAG, "Foto de Google cargada: $googlePhotoUrl")
                }
            } catch (e: Exception) {
                Log.w(TAG, "No se pudo cargar foto de Google: ${e.message}")
            }
        }
    }

    /**
     * Verifica si el perfil está completo
     */
    fun checkProfileComplete() {
        viewModelScope.launch {
            val isComplete = repository.isProfileComplete()
            _isProfileComplete.value = isComplete
            Log.d(TAG, "Perfil completo: $isComplete")
        }
    }

    /**
     * Actualiza los campos del formulario
     */
    fun onFirstNameChange(value: String) {
        _firstName.value = value
    }

    fun onLastNameChange(value: String) {
        _lastName.value = value
    }

    fun onPhoneNumberChange(value: String) {
        // Solo permitir números y ciertos caracteres
        if (value.all { it.isDigit() || it in listOf('+', '-', ' ', '(', ')') }) {
            _phoneNumber.value = value
        }
    }

    fun updateLocation(address: String, latitude: Double, longitude: Double, locationName: String = "") {
        _fullAddress.value = address
        _defaultPickupLatitude.value = latitude
        _defaultPickupLongitude.value = longitude
        _defaultPickupLocationName.value = locationName.ifEmpty { 
            // Extraer nombre corto de la dirección (primera parte antes de la coma)
            address.split(",").firstOrNull()?.trim() ?: "Mi ubicación"
        }
    }

    fun setDefaultPickupLocation(locationName: String, latitude: Double, longitude: Double) {
        _defaultPickupLocationName.value = locationName
        _defaultPickupLatitude.value = latitude
        _defaultPickupLongitude.value = longitude
    }

    /**
     * Sube una imagen de perfil
     */
    fun uploadProfileImage(imageUri: Uri) {
        viewModelScope.launch {
            _isUploadingImage.value = true
            _updateState.value = ProfileUpdateState(isLoading = true)

            val oldImageUrl = _profileImageUrl.value
            imageRepository.updateProfileImage(imageUri, oldImageUrl).fold(
                onSuccess = { newImageUrl ->
                    _profileImageUrl.value = newImageUrl
                    _isUploadingImage.value = false
                    _updateState.value = ProfileUpdateState(isLoading = false)
                    Log.d(TAG, "Imagen subida exitosamente: $newImageUrl")
                },
                onFailure = { error ->
                    _isUploadingImage.value = false
                    _updateState.value = ProfileUpdateState(
                        isLoading = false,
                        error = error.message ?: "Error al subir imagen"
                    )
                    Log.e(TAG, "Error al subir imagen", error)
                }
            )
        }
    }

    /**
     * Establece la URL de la imagen de perfil (para Google Sign-In)
     */
    fun setProfileImageUrl(url: String) {
        _profileImageUrl.value = url
        Log.d(TAG, "URL de imagen establecida: $url")
    }

    /**
     * Valida los campos del formulario
     */
    private fun validateForm(): String? {
        return when {
            _firstName.value.isBlank() -> "El nombre es requerido"
            _lastName.value.isBlank() -> "El apellido es requerido"
            _phoneNumber.value.isBlank() -> "El teléfono es requerido"
            _phoneNumber.value.length < 9 -> "El teléfono debe tener al menos 9 dígitos"
            _fullAddress.value.isBlank() -> "La dirección es requerida"
            _defaultPickupLocationName.value.isBlank() -> "Debes seleccionar tu ubicación de entrega predeterminada"
            else -> null
        }
    }

    /**
     * Guarda el perfil del usuario
     */
    fun saveProfile(onSuccess: () -> Unit) {
        val validationError = validateForm()
        if (validationError != null) {
            _updateState.value = ProfileUpdateState(error = validationError)
            return
        }

        viewModelScope.launch {
            _updateState.value = ProfileUpdateState(isLoading = true)

            val profile = UserProfile(
                firstName = _firstName.value.trim(),
                lastName = _lastName.value.trim(),
                phoneNumber = _phoneNumber.value.trim(),
                fullAddress = _fullAddress.value.trim(),
                profileImageUrl = _profileImageUrl.value,
                defaultPickupLocationName = _defaultPickupLocationName.value,
                defaultPickupLatitude = _defaultPickupLatitude.value,
                defaultPickupLongitude = _defaultPickupLongitude.value,
                isProfileComplete = true
            )

            repository.saveUserProfile(profile).fold(
                onSuccess = {
                    _userProfile.value = profile
                    _isProfileComplete.value = true
                    _updateState.value = ProfileUpdateState(
                        isLoading = false,
                        success = true
                    )
                    Log.d(TAG, "Perfil guardado exitosamente")
                    onSuccess()
                },
                onFailure = { error ->
                    _updateState.value = ProfileUpdateState(
                        isLoading = false,
                        error = "Error al guardar perfil: ${error.message}"
                    )
                    Log.e(TAG, "Error al guardar perfil", error)
                }
            )
        }
    }

    /**
     * Actualiza el perfil existente
     */
    fun updateProfile(onSuccess: () -> Unit) {
        val validationError = validateForm()
        if (validationError != null) {
            _updateState.value = ProfileUpdateState(error = validationError)
            return
        }

        viewModelScope.launch {
            _updateState.value = ProfileUpdateState(isLoading = true)

            val currentProfile = _userProfile.value ?: UserProfile()
            val updatedProfile = currentProfile.copy(
                firstName = _firstName.value.trim(),
                lastName = _lastName.value.trim(),
                phoneNumber = _phoneNumber.value.trim(),
                fullAddress = _fullAddress.value.trim(),
                profileImageUrl = _profileImageUrl.value,
                defaultPickupLocationName = _defaultPickupLocationName.value,
                defaultPickupLatitude = _defaultPickupLatitude.value,
                defaultPickupLongitude = _defaultPickupLongitude.value,
                isProfileComplete = true,
                updatedAt = System.currentTimeMillis()
            )

            repository.saveUserProfile(updatedProfile).fold(
                onSuccess = {
                    _userProfile.value = updatedProfile
                    _updateState.value = ProfileUpdateState(
                        isLoading = false,
                        success = true
                    )
                    Log.d(TAG, "Perfil actualizado exitosamente")
                    onSuccess()
                },
                onFailure = { error ->
                    _updateState.value = ProfileUpdateState(
                        isLoading = false,
                        error = "Error al actualizar perfil: ${error.message}"
                    )
                    Log.e(TAG, "Error al actualizar perfil", error)
                }
            )
        }
    }

    /**
     * Limpia el mensaje de error
     */
    fun clearError() {
        _updateState.value = _updateState.value.copy(error = null)
    }

    /**
     * Resetea el estado de éxito
     */
    fun resetSuccess() {
        _updateState.value = _updateState.value.copy(success = false)
    }
}
