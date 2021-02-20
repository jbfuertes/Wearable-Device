package com.ama.ece.wearablechildobserver

import android.net.wifi.WifiManager
import android.os.CountDownTimer
import android.util.Log
import com.ama.ece.wearablechildobserver.ConnectionHelper.Message.Companion.SIGNAL_STRENGTH
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.net.Socket
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException

class ConnectionHelper(private val view: View, private val wifiManager: WifiManager) {

    private val disposables = CompositeDisposable()
    private var socket: Socket? = null
    private var outputStream: DataOutputStream? = null
    private var inputStream: BufferedReader? = null

    private var signalThreshold: Message.SignalThreshold? = null

    private val countDownTimer = object : CountDownTimer(10000, 1000) {
        override fun onFinish() {
            view.onConnectionChange(false)
            disconnect()
            start()
        }

        override fun onTick(p0: Long) {
        }

    }

    init {
        start()
        monitorSignal()
    }

    fun disconnect() {
        socket?.close()
    }

    private fun observeMessages() {
        Observable.interval(100, TimeUnit.MILLISECONDS)
            .map { inputStream!!.readLine() }
            .filter { it.isNotBlank() }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe({
                if (it.isEmpty()) {
                    return@subscribe
                }
                val message = Message.getMessage(it)
                view.onMessage(message)
                if (message is Message.SignalThreshold) {
                    signalThreshold = message
                }
                Log.d("test", it)
                countDownTimer.cancel()
                countDownTimer.start()
            }, {
                disconnect()
                start()
                it.printStackTrace()
            }).addTo(disposables)
    }

    private fun monitorSignal() {
        Observable.interval(1000, TimeUnit.MILLISECONDS)
            .map { wifiManager.connectionInfo.rssi }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe({
                signalThreshold?.value?.let { threshold ->
                    view.onReachedSignalThreshold(it <= threshold)
                }
                sendMessage("$SIGNAL_STRENGTH$it")
                view.onSignalStrengthChange(it)
            }, {
                it.printStackTrace()
            }).addTo(disposables)
    }

    private fun start() {
        Completable.fromAction {
            socket = Socket(HOST_ADDRESS, PORT)
            socket?.let {
                it.keepAlive = true
                outputStream?.close()
                inputStream?.close()
                outputStream = DataOutputStream(it.getOutputStream())
                inputStream = BufferedReader(InputStreamReader(it.getInputStream()))
            }
        }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .doOnSubscribe { view.onConnecting() }
            .subscribe({
                observeMessages()
                view.onConnectionChange(true)
            }, {
                it.printStackTrace()
                disconnect()
                start()
            }).addTo(disposables)
    }

    fun sendMessage(message: String) {
        Completable.fromAction {
            val buf = message.toByteArray(charset("UTF-8"))
            outputStream?.write(buf, 0, buf.size)
        }
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeOn(Schedulers.io())
            .subscribe({

            }, {
                it.printStackTrace()
                view.onReachedSignalThreshold(true)
            }).addTo(disposables)
    }

    fun onDestroy() {
        disposables.clear()
    }

    companion object {
        private const val HOST_ADDRESS = "192.168.4.1"/*"192.168.1.6"*/
        private const val PORT = 80/*1024*/
    }

    sealed class Message {
        data class Battery(val value: Int) : Message()
        data class SignalThreshold(val value: Int) : Message()
        data class Detached(val detached: Int) : Message()
        object Undefined : Message()

        companion object {
            private const val BATTERY = "BATTERY: "
            private const val SIGNAL_THRESHOLD = "SIGNAL_THRESHOLD: "
            private const val DETACHED = "DETACHED: "
            const val SIGNAL_STRENGTH = "SIGNAL_STRENGTH: "
            const val STOP_ALARM = "STOP"
            const val RESET = "RESET"
            const val ALARM = "ALARM"

            fun getMessage(message: String): Message {
                return when {
                    message.contains(BATTERY) -> Battery(message.replace(BATTERY, "").toInt())

                    message.contains(SIGNAL_THRESHOLD) -> SignalThreshold(
                        message.replace(SIGNAL_THRESHOLD, "").toInt()
                    )

                    message.contains(DETACHED) -> Detached(message.replace(DETACHED, "").toInt())

                    else -> Undefined
                }
            }
        }
    }
}