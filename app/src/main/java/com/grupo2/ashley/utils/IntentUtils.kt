package com.grupo2.ashley.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.grupo2.ashley.R

/**
 * Abre Google Maps mostrando la ruta desde la ubicación actual hasta el destino
 */
fun openMapRoute(
    context: Context,
    destinationLat: Double,
    destinationLng: Double,
    destinationName: String = "Lugar de entrega"
) {
    try {
        // URI para Google Maps con ruta desde ubicación actual
        val uri = Uri.parse("https://www.google.com/maps/dir/?api=1&destination=$destinationLat,$destinationLng&travelmode=driving")
        
        val intent = Intent(Intent.ACTION_VIEW, uri).apply {
            setPackage("com.google.android.apps.maps")
        }
        
        // Verificar si Google Maps está instalado
        if (intent.resolveActivity(context.packageManager) != null) {
            context.startActivity(intent)
        } else {
            // Si no está instalado, abrir en el navegador
            val browserIntent = Intent(Intent.ACTION_VIEW, uri)
            context.startActivity(browserIntent)
        }
    } catch (e: Exception) {
        Toast.makeText(
            context,
            context.getString(R.string.error_mapa,{e.message}),
            Toast.LENGTH_SHORT
        ).show()
    }
}

/**
 * Abre el marcador del teléfono para llamar al número especificado
 */
fun makePhoneCall(context: Context, phoneNumber: String) {
    try {
        if (phoneNumber.isBlank()) {
            Toast.makeText(
                context,
                context.getString(R.string.error_telefono_vendedor),
                Toast.LENGTH_SHORT
            ).show()
            return
        }
        
        // Limpiar el número de teléfono (eliminar espacios, guiones, etc.)
        val cleanNumber = phoneNumber.replace(Regex("[^0-9+]"), "")
        
        val intent = Intent(Intent.ACTION_DIAL).apply {
            data = Uri.parse("tel:$cleanNumber")
        }
        
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(
            context,
            context.getString(R.string.error_marcador,{e.message}),
            Toast.LENGTH_SHORT
        ).show()
    }
}

/**
 * Calcula la distancia aproximada entre dos puntos en kilómetros
 * Usa la fórmula de Haversine
 */
fun calculateDistance(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double
): Double {
    val earthRadius = 6371.0 // Radio de la Tierra en km

    val dLat = Math.toRadians(lat2 - lat1)
    val dLon = Math.toRadians(lon2 - lon1)

    val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(dLon / 2) * Math.sin(dLon / 2)

    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))

    return earthRadius * c
}

/**
 * Formatea la distancia en un texto legible
 */
fun formatDistance(distanceKm: Double): String {
    return when {
        distanceKm < 1 -> "${(distanceKm * 1000).toInt()} metros"
        distanceKm < 10 -> String.format("%.1f km", distanceKm)
        else -> "${distanceKm.toInt()} km"
    }
}
