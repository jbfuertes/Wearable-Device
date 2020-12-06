package com.ama.ece.wearablechildobserver

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.AppCompatImageButton
import androidx.fragment.app.DialogFragment
import com.google.android.material.textview.MaterialTextView

class DialogNotif(
    private val type: NotificationType,
    private val onClose: () -> Unit
) : DialogFragment() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setStyle(STYLE_NO_TITLE, R.style.Dialog_Fullscreen)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_notif, container, false)
        dialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return view
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isCancelable = false
        view.apply {
            findViewById<AppCompatImageButton>(R.id.btn_close)?.setOnClickListener {
                onClose()
                dismiss()
            }

            findViewById<MaterialTextView>(R.id.txt_message)?.let {
                when (type) {
                    NotificationType.CHILD_NOTIF -> {
                        it.setBackgroundResource(R.drawable.bg_message_red)
                        it.text = getString(R.string.dialog_message_child_notification)
                    }

                    NotificationType.BATTERY -> {
                        it.setBackgroundResource(R.drawable.bg_message_red)
                        it.text = getString(R.string.dialog_message_battery_notification)
                    }

                    else -> {
                        it.setBackgroundResource(R.drawable.bg_message_yellow)
                        it.text = getString(R.string.dialog_message_detached_notification)
                    }
                }
            }
        }
    }

    enum class NotificationType {
        CHILD_NOTIF,
        DETACHED,
        BATTERY
    }
}