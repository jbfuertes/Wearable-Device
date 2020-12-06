package com.ama.ece.wearablechildobserver

import com.ama.ece.wearablechildobserver.ConnectionHelper.*

interface View {

    fun onConnecting()

    fun onConnectionChange(isConnected: Boolean)

    fun onMessage(message: Message)

    fun onSignalStrengthChange(it: Int)

    fun onReachedSignalThreshold(isReached: Boolean)

}