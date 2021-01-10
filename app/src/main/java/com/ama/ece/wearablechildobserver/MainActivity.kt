package com.ama.ece.wearablechildobserver

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.media.MediaPlayer
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiConfiguration
import android.net.wifi.WifiManager
import android.net.wifi.WifiNetworkSpecifier
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatImageButton
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.isVisible
import com.ama.ece.wearablechildobserver.ConnectionHelper.Message.Companion.ALARM
import com.ama.ece.wearablechildobserver.ConnectionHelper.Message.Companion.RESET
import com.ama.ece.wearablechildobserver.ConnectionHelper.Message.Companion.STOP_ALARM
import com.ama.ece.wearablechildobserver.databinding.ActivityMainBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textview.MaterialTextView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.addTo
import io.reactivex.schedulers.Schedulers
import java.util.concurrent.TimeUnit


class MainActivity : AppCompatActivity(), View {

    private lateinit var connectionHelper: ConnectionHelper

    private lateinit var viewBinding: ActivityMainBinding
    private lateinit var player: MediaPlayer

    private var shouldAllowAlarm = true
    private var isDisconnected = false
    private var isSignalThresholdReached = false
    private var isChildNotifClosed = false
    private var isDetachedNotifClosed = false
    private var isLowBatteryNotifClosed = false
    private var isDetached = false

    private val childNotifDialog = DialogNotif(DialogNotif.NotificationType.CHILD_NOTIF) {
        isChildNotifClosed = true
    }

    private val detachNotifDialog = DialogNotif(DialogNotif.NotificationType.DETACHED) {
        isDetachedNotifClosed = true
    }

    private val lowBatteryNotifDialog = DialogNotif(DialogNotif.NotificationType.BATTERY) {
        isLowBatteryNotifClosed = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityMainBinding.inflate(layoutInflater)
        player = MediaPlayer.create(this, Settings.System.DEFAULT_RINGTONE_URI)
        player.isLooping = true

        setContentView(viewBinding.root)
        setupInputs()

        setupWifiConnection()

    }

    override fun onDestroy() {
        super.onDestroy()
        connectionHelper.onDestroy()
    }

    private fun setupWifiConnection() {
        val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
        connectionHelper = ConnectionHelper(this, wifiManager)
    }

    private fun setupInputs() {
        viewBinding.apply {
            ivAlarm.setOnClickListener {
                connectionHelper.sendMessage(STOP_ALARM)
                shouldAllowAlarm = false
                checkAlarm()
            }

            btnReset.setOnClickListener {
                shouldAllowAlarm = true
                isChildNotifClosed = false
                isDetachedNotifClosed = false
                isLowBatteryNotifClosed = false
                connectionHelper.sendMessage(RESET)
            }

            tglBtnAlarm.setOnCheckedChangeListener { _, b ->
                connectionHelper.sendMessage(if (b) ALARM else RESET)
            }
        }
    }

    override fun onConnecting() {
        viewBinding.prgBar.isVisible = true
    }

    private fun checkAlarm() {
        if ((isDetached || isDisconnected || isSignalThresholdReached) && shouldAllowAlarm) {
            if (player.isPlaying.not()) {
                player.reset()
                player.setDataSource(this, Settings.System.DEFAULT_RINGTONE_URI)
                player.prepare()
                player.start()
                viewBinding.ivAlarm.setColorFilter(Color.RED)
            }
        } else {
            player.stop()
            viewBinding.ivAlarm.setColorFilter(Color.parseColor("#3B8C2C"))
        }
    }

    override fun onConnectionChange(isConnected: Boolean) {
        viewBinding.prgBar.isVisible = isConnected.not()
        checkAlarm()
    }

    @SuppressLint("SetTextI18n")
    override fun onMessage(message: ConnectionHelper.Message) {
        if (message is ConnectionHelper.Message.Battery) {
            viewBinding.apply {
                tvBatteryPercentage.text = "${message.value}%"
                ivBattery.setImageResource(
                    when (message.value) {
                        in 0..10 -> R.drawable.ic_battery_0

                        in 11..29 -> R.drawable.ic_battery_20

                        in 30..40 -> R.drawable.ic_battery_30

                        in 41..59 -> R.drawable.ic_battery_50

                        in 60..70 -> R.drawable.ic_battery_60

                        in 71..89 -> R.drawable.ic_battery_90

                        else -> R.drawable.ic_battery_100
                    }
                )

                if (message.value <= 20 && isLowBatteryNotifClosed.not() && lowBatteryNotifDialog.isVisible.not()) {
                    lowBatteryNotifDialog.show(supportFragmentManager, null)
                }
            }
        }

        if (message is ConnectionHelper.Message.Detached) {
            isDetached = message.detached == 1
            if (isDetached && isDetachedNotifClosed.not() && detachNotifDialog.isVisible.not()) {
                detachNotifDialog.show(supportFragmentManager, null)
            }
            checkAlarm()
        }
    }

    override fun onSignalStrengthChange(it: Int) {
        viewBinding.ivSignalStr.setImageResource(
            when {
                it > -50 -> R.drawable.ic_signal_wifi_4

                it in -50 downTo -60 -> R.drawable.ic_signal_wifi_3

                it in -60 downTo -70 -> R.drawable.ic_signal_wifi_2

                it in -70 downTo -80 -> R.drawable.ic_signal_wifi_1

                else -> R.drawable.ic_signal_wifi_0
            }
        )
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    override fun onReachedSignalThreshold(isReached: Boolean) {
        isSignalThresholdReached = isReached
        if (isReached && isChildNotifClosed.not() && childNotifDialog.isVisible.not()) {
            childNotifDialog.show(supportFragmentManager, null)
        }
        checkAlarm()
    }
}