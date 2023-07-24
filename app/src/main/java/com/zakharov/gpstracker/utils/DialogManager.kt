package com.zakharov.gpstracker.utils

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.widget.Toast
import com.zakharov.gpstracker.R
import com.zakharov.gpstracker.databinding.SaveDialogBinding
import com.zakharov.gpstracker.db.TrackItem

object DialogManager {
    fun showLocEnabledDialog(context: Context, listener: Listener) {
        val builder = AlertDialog.Builder(context)
        val dialog = builder.create()
        dialog.setTitle(R.string.location_disabled)
        dialog.setMessage(context.getString(R.string.location_dialog_message))
        dialog.setButton(AlertDialog.BUTTON_POSITIVE, context.getString(R.string.yes)) { _, _ ->
            listener.onClick()
        }

        dialog.setButton(AlertDialog.BUTTON_NEGATIVE, context.getString(R.string.no)) { _, _ ->
            dialog.dismiss()
        }
        dialog.show()
    }

    fun showSaveDialog(context: Context, item: TrackItem?, listener: Listener) {
        val builder = AlertDialog.Builder(context)
        val binding = SaveDialogBinding.inflate(LayoutInflater.from(context), null, false)
        builder.setView(binding.root)
        val dialog = builder.create()
        binding.apply {
            val time = "${item?.time}"
            val speed = "${item?.speed} km/h"
            val distance = "${item?.distance} km"
            tvTime.text = time
            tvSpeed.text = speed
            tvDistance.text = distance
            bSave.setOnClickListener {
                listener.onClick()
                dialog.dismiss()
            }
            bCansel.setOnClickListener {
                dialog.dismiss()

            }
        }
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.show()
    }

    interface Listener {
        fun onClick()
    }
}