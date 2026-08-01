package com.quangthe.nhacnho_uongthuoc

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.content.res.AppCompatResources
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.res.ResourcesCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class Dialogs(private val context: Context) {

    private val toasts = Toasts(context)
    private val sharedPrefs = SharedPrefs(context)

    private val isDarkMode: Boolean
        get() = sharedPrefs.darkDialogsPref

    val welcomeDialog: Dialog
        get() = createWelcomeDialog()

    val donationDialog: Dialog
        get() = createDonationDialog()

    fun getCrashDialog(error: String?): Dialog {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_crash, null)
        val dialog = AlertDialog.Builder(context).setView(dialogView).create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(0))

        val dialogLayout = dialogView.findViewById<ConstraintLayout>(R.id.custom_dialog_constraint_layout)
        val titleTextView = dialogView.findViewById<TextView>(R.id.dialog_title_textview)
        val messageTextView = dialogView.findViewById<TextView>(R.id.dialog_message_textview)
        val rightButton = dialogView.findViewById<Button>(R.id.btnYes)
        val leftButton = dialogView.findViewById<Button>(R.id.btnNo)

        if (isDarkMode) {
            dialogLayout.background = AppCompatResources.getDrawable(context, R.drawable.dialog_background_dark)
            titleTextView.background = AppCompatResources.getDrawable(context, R.drawable.dialog_title_background_dark)
            messageTextView.setTextColor(ResourcesCompat.getColor(context.resources, R.color.alice_blue, null))
            rightButton.background = AppCompatResources.getDrawable(context, R.drawable.dialog_bottom_btn_dark)
            leftButton.background = AppCompatResources.getDrawable(context, R.drawable.dialog_bottom_btn_purple)
        } else {
            rightButton.background = AppCompatResources.getDrawable(context, R.drawable.dialog_bottom_btn_green)
            leftButton.background = AppCompatResources.getDrawable(context, R.drawable.dialog_bottom_btn_purple)
        }

        titleTextView.text = context.getString(R.string.crash_dialog_title)
        messageTextView.text = error
        leftButton.setText(R.string.report)
        rightButton.setText(R.string.ok)
        
        leftButton.setOnClickListener {
            val selectorIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
            }

            val emailAddress = "simpilldev@gmail.com"
            val emailTitle = "Simpill Crash Report"
            val emailBody = "Hi Stephen,\n\nI encountered an error while using Simpill. Here is the error log:\n\n$error"

            val emailIntent = Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_EMAIL, arrayOf(emailAddress))
                putExtra(Intent.EXTRA_SUBJECT, emailTitle)
                putExtra(Intent.EXTRA_TEXT, emailBody)
                selector = selectorIntent
            }

            context.startActivity(Intent.createChooser(emailIntent, "Send error report..."))
        }
        rightButton.setOnClickListener { dialog.dismiss() }
        return dialog
    }

    private fun createWelcomeDialog(): Dialog {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_welcome, null)
        val dialog = AlertDialog.Builder(context).setView(dialogView).create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(0))

        val dialogLayout = dialogView.findViewById<ConstraintLayout>(R.id.custom_dialog_constraint_layout)
        val titleTextView = dialogView.findViewById<TextView>(R.id.dialog_title_textview)
        val messageTextView = dialogView.findViewById<TextView>(R.id.dialog_message_textview)

        if (isDarkMode) {
            dialogLayout.background = AppCompatResources.getDrawable(context, R.drawable.dialog_background_dark)
            titleTextView.background = AppCompatResources.getDrawable(context, R.drawable.dialog_title_background_dark)
            messageTextView.setTextColor(ResourcesCompat.getColor(context.resources, R.color.alice_blue, null))
        }

        val welcomeBtn = dialogView.findViewById<Button>(R.id.done_btn)
        welcomeBtn.setOnClickListener { dialog.dismiss() }
        dialog.setOnDismissListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(context)) {
                getPermissionOverlayDialog().show()
            }
        }

        return dialog
    }

    private fun createDonationDialog(): Dialog {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog_donate, null)
        val dialog = AlertDialog.Builder(context).setView(dialogView).create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(0))

        val dialogLayout = dialogView.findViewById<ConstraintLayout>(R.id.donateConstraint)
        val titleTextView = dialogView.findViewById<TextView>(R.id.dialog_title_textview)
        val messageTextView = dialogView.findViewById<TextView>(R.id.dialog_message_textview)
        val paypalDonation = dialogView.findViewById<ImageButton>(R.id.imageButton)
        val paypalDonationTextView = dialogView.findViewById<TextView>(R.id.textView5)
        val bitcoinBtn = dialogView.findViewById<ImageButton>(R.id.imageButton3)
        val bitcoinTextView = dialogView.findViewById<TextView>(R.id.textView6)
        val moneroBtn = dialogView.findViewById<ImageButton>(R.id.imageButton4)
        val moneroTextView = dialogView.findViewById<TextView>(R.id.textView7)
        val dismissBtn = dialogView.findViewById<Button>(R.id.btnNo)

        if (isDarkMode) {
            val color = ResourcesCompat.getColor(context.resources, R.color.alice_blue, null)
            dialogLayout.background = AppCompatResources.getDrawable(context, R.drawable.dialog_background_dark)
            titleTextView.background = AppCompatResources.getDrawable(context, R.drawable.dialog_title_background_dark)
            messageTextView.setTextColor(color)
            paypalDonationTextView.setTextColor(color)
            moneroTextView.setTextColor(color)
            bitcoinTextView.setTextColor(color)
            dismissBtn.background = AppCompatResources.getDrawable(context, R.drawable.dialog_bottom_btn_dark)
        }
        
        val clipboardHelper = ClipboardHelper()
        val openPaypalDonationLink = Intent(Intent.ACTION_VIEW, Uri.parse(context.getString(R.string.paypal_donation_link)))

        val paypalListener = View.OnClickListener { context.startActivity(openPaypalDonationLink) }
        paypalDonation.setOnClickListener(paypalListener)
        paypalDonationTextView.setOnClickListener(paypalListener)

        bitcoinBtn.setOnClickListener { clipboardHelper.copyAddressToClipboard(context, ClipboardHelper.BTC) }
        bitcoinTextView.setOnClickListener { clipboardHelper.copyAddressToClipboard(context, ClipboardHelper.BTC) }
        
        moneroBtn.setOnClickListener { clipboardHelper.copyAddressToClipboard(context, ClipboardHelper.XMR) }
        moneroTextView.setOnClickListener { clipboardHelper.copyAddressToClipboard(context, ClipboardHelper.XMR) }
        
        dismissBtn.setOnClickListener { dialog.dismiss() }
        
        return dialog
    }

    @SuppressLint("InlinedApi")
    fun getPermissionOverlayDialog(): Dialog {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog, null)
        val dialog = AlertDialog.Builder(context).setView(dialogView).create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(0))

        val dialogLayout = dialogView.findViewById<ConstraintLayout>(R.id.custom_dialog_constraint_layout)
        val titleTextView = dialogView.findViewById<TextView>(R.id.dialog_title_textview)
        val messageTextView = dialogView.findViewById<TextView>(R.id.dialog_message_textview)
        val rightButton = dialogView.findViewById<Button>(R.id.btnYes)
        val leftButton = dialogView.findViewById<Button>(R.id.btnNo)

        if (isDarkMode) {
            dialogLayout.background = AppCompatResources.getDrawable(context, R.drawable.dialog_background_dark)
            titleTextView.background = AppCompatResources.getDrawable(context, R.drawable.dialog_title_background_dark)
            messageTextView.setTextColor(ResourcesCompat.getColor(context.resources, R.color.alice_blue, null))
            rightButton.background = AppCompatResources.getDrawable(context, R.drawable.dialog_bottom_btn_dark)
            leftButton.background = AppCompatResources.getDrawable(context, R.drawable.dialog_bottom_btn_dark)
        } else {
            titleTextView.background = AppCompatResources.getDrawable(context, R.drawable.dialog_title_background_purple)
            rightButton.background = AppCompatResources.getDrawable(context, R.drawable.dialog_bottom_btn_purple)
            leftButton.background = AppCompatResources.getDrawable(context, R.drawable.dialog_bottom_btn_red)
        }

        titleTextView.text = context.getString(R.string.ask_overlay_permission_dialog_title)
        messageTextView.text = context.getString(R.string.ask_overlay_permission_dialog_message)
        leftButton.text = context.getString(R.string.dismiss)
        rightButton.text = context.getString(R.string.settings)

        rightButton.setOnClickListener {
            context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION))
            dialog.dismiss()
        }
        leftButton.setOnClickListener { dialog.dismiss() }
        return dialog
    }

    fun getPillResetDialog(pill: Pill, position: Int): Dialog {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog, null)
        val dialog = AlertDialog.Builder(context).setView(dialogView).create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(0))

        val dialogLayout = dialogView.findViewById<ConstraintLayout>(R.id.custom_dialog_constraint_layout)
        val titleTextView = dialogView.findViewById<TextView>(R.id.dialog_title_textview)
        val messageTextView = dialogView.findViewById<TextView>(R.id.dialog_message_textview)
        val rightButton = dialogView.findViewById<Button>(R.id.btnYes)
        val leftButton = dialogView.findViewById<Button>(R.id.btnNo)

        if (isDarkMode) {
            dialogLayout.background = AppCompatResources.getDrawable(context, R.drawable.dialog_background_dark)
            titleTextView.background = AppCompatResources.getDrawable(context, R.drawable.dialog_title_background_dark)
            messageTextView.setTextColor(ResourcesCompat.getColor(context.resources, R.color.alice_blue, null))
            rightButton.background = AppCompatResources.getDrawable(context, R.drawable.dialog_bottom_btn_red)
            leftButton.background = AppCompatResources.getDrawable(context, R.drawable.dialog_bottom_btn_dark)
        } else {
            rightButton.background = AppCompatResources.getDrawable(context, R.drawable.dialog_bottom_btn_red)
            leftButton.background = AppCompatResources.getDrawable(context, R.drawable.dialog_bottom_btn_purple)
        }

        titleTextView.text = context.getString(R.string.reset_pill_dialog_title)
        messageTextView.text = context.getString(R.string.reset_pill_dialog_message, pill.name)
        rightButton.text = context.getString(R.string.yes)
        leftButton.text = context.getString(R.string.no)

        rightButton.setOnClickListener {
            pill.resetPill(context, position)
            CoroutineScope(Dispatchers.IO).launch {
                AppDatabase.getDatabase(context).pillDao().updatePillSync(pill)
            }
            dialog.dismiss()
        }
        leftButton.setOnClickListener { dialog.dismiss() }
        return dialog
    }

    fun getPillDeletionDialog(pill: Pill, position: Int): Dialog {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog, null)
        val dialog = AlertDialog.Builder(context).setView(dialogView).create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(0))

        val dialogLayout = dialogView.findViewById<ConstraintLayout>(R.id.custom_dialog_constraint_layout)
        val titleTextView = dialogView.findViewById<TextView>(R.id.dialog_title_textview)
        val messageTextView = dialogView.findViewById<TextView>(R.id.dialog_message_textview)
        val rightButton = dialogView.findViewById<Button>(R.id.btnYes)
        val leftButton = dialogView.findViewById<Button>(R.id.btnNo)

        if (isDarkMode) {
            dialogLayout.background = AppCompatResources.getDrawable(context, R.drawable.dialog_background_dark)
            titleTextView.background = AppCompatResources.getDrawable(context, R.drawable.dialog_title_background_dark)
            messageTextView.setTextColor(ResourcesCompat.getColor(context.resources, R.color.alice_blue, null))
            rightButton.background = AppCompatResources.getDrawable(context, R.drawable.dialog_bottom_btn_red)
            leftButton.background = AppCompatResources.getDrawable(context, R.drawable.dialog_bottom_btn_dark)
        } else {
            titleTextView.background = AppCompatResources.getDrawable(context, R.drawable.dialog_title_background_red)
            rightButton.background = AppCompatResources.getDrawable(context, R.drawable.dialog_bottom_btn_red)
            leftButton.background = AppCompatResources.getDrawable(context, R.drawable.dialog_bottom_btn_purple)
        }

        titleTextView.text = context.getString(R.string.pill_deletion_dialog_title)
        messageTextView.text = context.getString(R.string.pill_deletion_dialog_message, pill.name)
        rightButton.text = context.getString(R.string.yes)
        leftButton.text = context.getString(R.string.no)

        rightButton.setOnClickListener {
            pill.cancelAlarms(context)
            CoroutineScope(Dispatchers.IO).launch {
                AppDatabase.getDatabase(context).pillDao().softDeletePillSync(pill.primaryKey)
                withContext(Dispatchers.Main) {
                    if (context is Pill.PillListener) {
                        context.notifyDeletedPill(pill, position)
                    }
                }
            }
            toasts.showCustomToast(context.getString(R.string.append_pill_deleted_toast, pill.name))
            dialog.dismiss()
        }
        leftButton.setOnClickListener { dialog.dismiss() }

        return dialog
    }

    fun getDatabaseDeletionDialog(): Dialog {
        val dialogView = LayoutInflater.from(context).inflate(R.layout.dialog, null)
        val dialog = AlertDialog.Builder(context).setView(dialogView).create()
        dialog.window?.setBackgroundDrawable(ColorDrawable(0))

        val dialogLayout = dialogView.findViewById<ConstraintLayout>(R.id.custom_dialog_constraint_layout)
        val titleTextView = dialogView.findViewById<TextView>(R.id.dialog_title_textview)
        val messageTextView = dialogView.findViewById<TextView>(R.id.dialog_message_textview)
        val rightButton = dialogView.findViewById<Button>(R.id.btnYes)
        val leftButton = dialogView.findViewById<Button>(R.id.btnNo)

        if (isDarkMode) {
            dialogLayout.background = AppCompatResources.getDrawable(context, R.drawable.dialog_background_dark)
            titleTextView.background = AppCompatResources.getDrawable(context, R.drawable.dialog_title_background_red)
            messageTextView.setTextColor(ResourcesCompat.getColor(context.resources, R.color.alice_blue, null))
            rightButton.background = AppCompatResources.getDrawable(context, R.drawable.dialog_bottom_btn_red)
            leftButton.background = AppCompatResources.getDrawable(context, R.drawable.dialog_bottom_btn_dark)
        } else {
            titleTextView.background = AppCompatResources.getDrawable(context, R.drawable.dialog_title_background_red)
            rightButton.background = AppCompatResources.getDrawable(context, R.drawable.dialog_bottom_btn_red)
            leftButton.background = AppCompatResources.getDrawable(context, R.drawable.dialog_bottom_btn_purple)
        }

        titleTextView.text = context.getString(R.string.pill_db_reset_dialog_title)
        messageTextView.text = context.getString(R.string.pill_db_reset_dialog_message)
        rightButton.text = context.getString(R.string.yes)
        leftButton.text = context.getString(R.string.no)

        rightButton.setOnClickListener {
            CoroutineScope(Dispatchers.IO).launch {
                AppDatabase.getDatabase(context).clearAllTables()
            }
            dialog.dismiss()
            toasts.showCustomToast(context.getString(R.string.pill_db_deleted_toast))
        }
        leftButton.setOnClickListener { dialog.dismiss() }

        return dialog
    }
}
