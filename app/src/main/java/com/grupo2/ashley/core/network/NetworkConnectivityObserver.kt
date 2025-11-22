package com.grupo2.ashley.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

interface ConnectivityObserver {

    // Observa continuamente el estado de conexión y emite cambios como un Flow
    fun observe(): Flow<Status>

    // Retorna inmediatamente si el dispositivo está conectado a Internet
    fun isConnected(): Boolean

    enum class Status {
        Available,
        Unavailable,
        Losing,
        Lost
    }
}

@Singleton
class NetworkConnectivityObserver @Inject constructor(
    @ApplicationContext private val context: Context
) : ConnectivityObserver {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    // Observa los cambios de conectividad usando NetworkCallback y los expone como Flow
    override fun observe(): Flow<ConnectivityObserver.Status> {
        return callbackFlow {
            val callback = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) {
                    super.onAvailable(network)
                    trySend(ConnectivityObserver.Status.Available)
                }

                override fun onLosing(network: Network, maxMsToLive: Int) {
                    super.onLosing(network, maxMsToLive)
                    trySend(ConnectivityObserver.Status.Losing)
                }

                override fun onLost(network: Network) {
                    super.onLost(network)
                    trySend(ConnectivityObserver.Status.Lost)
                }

                override fun onUnavailable() {
                    super.onUnavailable()
                    trySend(ConnectivityObserver.Status.Unavailable)
                }
            }

            val request = NetworkRequest.Builder()
                .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                .build()

            connectivityManager.registerNetworkCallback(request, callback)

            // Enviar estado inicial al flow
            trySend(if (isConnected()) ConnectivityObserver.Status.Available else ConnectivityObserver.Status.Unavailable)

            awaitClose {
                connectivityManager.unregisterNetworkCallback(callback)
            }
        }.distinctUntilChanged()
    }

    // Verifica de forma inmediata si hay una red activa con capacidad de Internet
    override fun isConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}
