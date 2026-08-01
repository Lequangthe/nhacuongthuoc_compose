package com.quangthe.nhacnho_uongthuoc

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import org.joda.time.DateTime

object AlarmHelper {

    fun setAlarm(context: Context, pill: Pill) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmReminderTimes = pill.getAlarmReminderTimes()
        val alarmRequestCodes = pill.getAlarmRequestCodes()
        val dateTimeManager = DateTimeManager()

        for (index in alarmReminderTimes.indices) {
            val freq = pill.frequency
            val requestCode = alarmRequestCodes[index]
            val reminderTime = alarmReminderTimes[index]

            Log.d("PILL_ALARM", "at " + dateTimeManager.formatLongAsDateTimeString(reminderTime))
            Log.d("PILL_ALARM", "with request code $requestCode")

            val pillAlarmPendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                Intent(context, ReceiverPillAlarm::class.java)
                    .putExtra(Pill.PRIMARY_KEY_INTENT_KEY_STRING, pill.primaryKey)
                    .putExtra(Pill.NOTIFICATION_ID_INTENT_KEY_STRING, requestCode),
                PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(pillAlarmPendingIntent)

            val interval = AlarmManager.INTERVAL_DAY * Math.max(1, freq)
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP, reminderTime, interval, pillAlarmPendingIntent
            )
        }
        
        // Auto-reset alarm
        val resetRequestCode = pill.primaryKey * 1000 + 999
        val startAutoResetReceiver = Intent(context, ReceiverPillAutoReset::class.java)
            .putExtra(Pill.PRIMARY_KEY_INTENT_KEY_STRING, pill.primaryKey)
            .putExtra(Pill.NOTIFICATION_ID_INTENT_KEY_STRING, resetRequestCode)

        val autoResetPendingIntent = PendingIntent.getBroadcast(
            context,
            resetRequestCode,
            startAutoResetReceiver,
            PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(autoResetPendingIntent)

        val resetInterval = AlarmManager.INTERVAL_DAY * Math.max(1, pill.frequency)
        val resetTime: Long = if (pill.frequency <= 1) {
            dateTimeManager.convertTimeToCurrentDateTimeInMillis("00:01")
        } else {
            val nextMedTime = alarmReminderTimes[0]
            var time = DateTime(nextMedTime).withTime(0, 1, 0, 0).millis
            while (time <= System.currentTimeMillis()) {
                time += resetInterval
            }
            time
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP, resetTime, resetInterval, autoResetPendingIntent
        )
    }

    fun cancelAlarms(context: Context, pill: Pill) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val alarmRequestCodes = pill.getAlarmRequestCodes()

        for (requestCode in alarmRequestCodes) {
            val pillAlarmPendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                Intent(context, ReceiverPillAlarm::class.java)
                    .putExtra(Pill.PRIMARY_KEY_INTENT_KEY_STRING, pill.primaryKey)
                    .putExtra(Pill.NOTIFICATION_ID_INTENT_KEY_STRING, requestCode),
                PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.cancel(pillAlarmPendingIntent)
        }

        // Cancel supply alarm
        val startPillSupplyReceiver = Intent(context, ReceiverPillSupply::class.java)
            .putExtra(Pill.PRIMARY_KEY_INTENT_KEY_STRING, pill.primaryKey)

        val pillSupplyPendingIntent = PendingIntent.getBroadcast(
            context,
            pill.primaryKey,
            startPillSupplyReceiver,
            PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pillSupplyPendingIntent)
    }

    fun setStockupAlarm(context: Context, pill: Pill) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val dateTimeManager = DateTimeManager()
        val sDate = pill.calculateStockupDate()

        if (sDate != Pill.NULL_DB_ENTRY_STRING) {
            val startPillSupplyReceiver = Intent(context, ReceiverPillSupply::class.java)
                .putExtra(Pill.PRIMARY_KEY_INTENT_KEY_STRING, pill.primaryKey)

            val pillSupplyPendingIntent = PendingIntent.getBroadcast(
                context,
                pill.primaryKey,
                startPillSupplyReceiver,
                PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(pillSupplyPendingIntent)

            var supplyReminderTime = dateTimeManager.formatDateTimeStringAsLong("$sDate 12:00")
            if (supplyReminderTime < System.currentTimeMillis()) {
                supplyReminderTime += 2629746000L // 1 month approx
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP, supplyReminderTime, pillSupplyPendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP, supplyReminderTime, pillSupplyPendingIntent
                )
            }
        }
    }
}
