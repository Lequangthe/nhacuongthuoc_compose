package com.quangthe.nhacnho_uongthuoc

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.Notification
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Parcelable
import android.provider.Settings
import android.util.Log
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.parcelize.IgnoredOnParcel
import kotlinx.parcelize.Parcelize
import org.joda.time.DateTime

@Parcelize
@Entity(tableName = "PillList")
data class Pill @JvmOverloads constructor(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "PrimaryKey")
    var primaryKey: Int = 0,

    @ColumnInfo(name = "PillName")
    var name: String = "",

    @ColumnInfo(name = "PillTime")
    var times24HrFormat: String = NULL_DB_ENTRY_STRING,

    @ColumnInfo(name = "PillFrequency")
    var frequency: Int = 1,

    @ColumnInfo(name = "StartDate")
    var startDate: String? = NULL_DB_ENTRY_STRING,

    @ColumnInfo(name = "PillStockup")
    var stockupDate: String? = NULL_DB_ENTRY_STRING,

    @ColumnInfo(name = "PillSupply")
    var supply: Int = -1,

    @ColumnInfo(name = "IsPillTaken")
    var taken: Int = 0,

    @ColumnInfo(name = "TimeTaken")
    var timeTaken: String? = NULL_DB_ENTRY_STRING,

    @ColumnInfo(name = "AlarmsSet")
    var alarmsSet: Int = 0,

    @ColumnInfo(name = "BottleColor")
    var bottleColor: Int = 2,

    @ColumnInfo(name = "CustomAlarmUri")
    var customAlarmUriString: String = "android.resource://com.quangthe.nhacnho_uongthuoc/" + R.raw.eas_alarm,

    @ColumnInfo(name = "AlarmType")
    var alarmType: Int = ALARM,

    @ColumnInfo(name = "ActualTimes")
    var actualTimes: String = "", // Lưu giờ thực tế uống, vd: "07:05, 12:10"

    @ColumnInfo(name = "IsDeleted")
    var isDeleted: Int = 0
) : Parcelable {

    @Ignore
    @IgnoredOnParcel
    private var _dateTimeManager: DateTimeManager? = null

    @delegate:Ignore
    @IgnoredOnParcel
    private val dateTimeManager: DateTimeManager by lazy {
        DateTimeManager()
    }

    @Ignore
    @IgnoredOnParcel
    var formattedStartDate: String = NULL_DB_ENTRY_STRING
        private set

    @Ignore
    @IgnoredOnParcel
    var times12HrFormat: String = NULL_DB_ENTRY_STRING
        private set

    @Ignore
    @IgnoredOnParcel
    private var alarmReminderTimes: LongArray = LongArray(0)

    @Ignore
    @IgnoredOnParcel
    private var alarmRequestCodes: IntArray = IntArray(0)

    init {
        updateDerivedFields()
    }

    /**
     * Cập nhật các trường phái sinh (derived fields) dựa trên dữ liệu hiện tại.
     * Thường được gọi sau khi thay đổi các giá trị cơ bản như startDate, timesArray, frequency.
     */
    fun updateDerivedFields() {
        if (startDate != null && startDate != NULL_DB_ENTRY_STRING) {
            formattedStartDate = dateTimeManager.convertISODateStringToLocallyFormattedString(startDate!!)
        }

        val times = timesArray
        if (times24HrFormat == NULL_DB_ENTRY_STRING || times24HrFormat.isEmpty()
            || times.isEmpty() || times.all { it.isBlank() }
        ) {
            return
        }

        val arrayHelper = ArrayHelper()
        times12HrFormat = arrayHelper.convertArrayToString(
            arrayHelper.convert24HrArrayTo12HrArray(times.clone())
        )

        setAlarmReminderTimes()
        setAlarmRequestCodes()
    }

    @Ignore
    constructor(
        name: String,
        timesArray: Array<String>,
        startDate: String?,
        stockupDate: String?,
        customAlarmUri: Uri?,
        frequency: Int,
        taken: Int,
        timeTaken: String?,
        supply: Int,
        alarmType: Int,
        alarmsSet: Int,
        bottleColor: Int
    ) : this(
        primaryKey = 0,
        name = name,
        startDate = startDate ?: NULL_DB_ENTRY_STRING,
        frequency = frequency,
        stockupDate = stockupDate ?: NULL_DB_ENTRY_STRING,
        customAlarmUriString = (customAlarmUri ?: DEFAULT_ALARM_URI).toString(),
        taken = taken,
        timeTaken = timeTaken ?: NULL_DB_ENTRY_STRING,
        supply = supply,
        alarmType = alarmType,
        alarmsSet = alarmsSet,
        bottleColor = bottleColor
    ) {
        this.timesArray = timesArray
        updateDerivedFields()
    }

    var timesArray: Array<String>
        @JvmName("getTimesArray")
        get() = if (times24HrFormat == NULL_DB_ENTRY_STRING || times24HrFormat.isEmpty()) {
            arrayOf("")
        } else {
            times24HrFormat.split(ArrayHelper.STR_SEPARATOR).toTypedArray()
        }
        set(value) {
            val arrayHelper = ArrayHelper()
            times24HrFormat = arrayHelper.convertArrayToString(value.clone())
            times12HrFormat = arrayHelper.convertArrayToString(
                arrayHelper.convert24HrArrayTo12HrArray(value.clone())
            )
            setAlarmReminderTimes()
        }

    fun getCustomAlarmUri(): Uri = Uri.parse(customAlarmUriString)

    fun setCustomAlarmUri(uri: Uri?) {
        customAlarmUriString = (uri ?: DEFAULT_ALARM_URI).toString()
    }

    fun takePill(context: Context, doseIndex: Int) {
        Log.d("PillTestSpy", "CRITICAL: takePill() is being called for dose $doseIndex! Checking stack trace...")
        Log.d("PillTestSpy", Log.getStackTraceString(Exception()))
        if (supply > 0) {
            supply -= 1
        }
        setDoseTaken(doseIndex, true)
        
        // Lưu giờ thực tế vào chuỗi actualTimes
        val currentTime = dateTimeManager.currentTimeString
        val actualArr = if (actualTimes.isBlank()) {
            Array(timesArray.size) { NULL_DB_ENTRY_STRING }
        } else {
            val parts = actualTimes.split(ArrayHelper.STR_SEPARATOR)
            // Đảm bảo kích thước mảng khớp với số lượng cữ
            Array(timesArray.size) { i -> parts.getOrNull(i) ?: NULL_DB_ENTRY_STRING }
        }
        
        if (doseIndex < actualArr.size) {
            actualArr[doseIndex] = currentTime
            actualTimes = ArrayHelper().convertArrayToString(actualArr)
        }

        timeTaken = currentTime
    }

    fun isDoseTaken(index: Int): Boolean {
        return (taken and (1 shl index)) != 0
    }

    fun setDoseTaken(index: Int, isTaken: Boolean) {
        taken = if (isTaken) {
            taken or (1 shl index)
        } else {
            taken and (1 shl index).inv()
        }
    }

    fun resetPill(context: Context, recyclerViewPosition: Int) {
        val bitsSet = Integer.bitCount(taken)
        if (supply >= 0) {
            supply += bitsSet
        }
        taken = PILL_NOT_TAKEN_VALUE
        timeTaken = NULL_DB_ENTRY_STRING
        actualTimes = "" // Xóa sạch giờ thực tế
        setAlarm(context)
        alarmsSet = 1
        (context as? PillListener)?.notifyResetPill(recyclerViewPosition)
    }

    fun autoResetPill(context: Context) {
        taken = PILL_NOT_TAKEN_VALUE
        timeTaken = NULL_DB_ENTRY_STRING
        actualTimes = "" // Xóa sạch giờ thực tế
        alarmsSet = 1
    }

    fun sendPillNotificationONLY_FOR_TEST(context: Context, doseIndex: Int) {
        Log.d("PillTestSpy", ">>> sendPillNotificationONLY_FOR_TEST called for dose: $doseIndex")
        val pillNotificationManagerCompat = NotificationManagerCompat.from(context)
        
        val requestCode = getAlarmRequestCodes()[doseIndex]
        
        val openMainIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, requestCode, openMainIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val pillReminderNotification = NotificationCompat.Builder(context, Simpill.PILL_REMINDER_CHANNEL)
            .setSmallIcon(R.drawable.pill_bottle_color_2)
            .setContentTitle("[TEST] $name")
            .setContentText("Đây là thông báo giả cữ " + (if (doseIndex < timesArray.size) timesArray[doseIndex] else ""))
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)
            .build()

        pillNotificationManagerCompat.notify("TEST_$name", requestCode, pillReminderNotification)
    }

    @SuppressLint("LaunchActivityFromNotification")
    fun sendPillNotification(context: Context, doseIndex: Int) {
        Log.d("PillNotify", "Sending notification for dose $doseIndex")
        
        if (isDoseTaken(doseIndex)) {
            Log.d("PillNotify", "Dose $doseIndex was already taken manually. Skipping.")
            return
        }

        val pillNotificationManagerCompat = NotificationManagerCompat.from(context)
        val requestCode = getAlarmRequestCodes()[doseIndex]

        val isTest = doseIndex < 0

        if (!isTest && (alarmType == ALARM || alarmType == CUSTOM_ALARM)
            && Build.VERSION.SDK_INT >= Build.VERSION_CODES.M
            && Settings.canDrawOverlays(context)) {
            context.startActivity(
                Intent(context, PillAlarmDisplay::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    .putExtra(PRIMARY_KEY_INTENT_KEY_STRING, primaryKey)
                    .putExtra(NOTIFICATION_ID_INTENT_KEY_STRING, requestCode)
            )
        }

        val sticky = SharedPrefs(context).stickyNotificationsPref

        val openMainIntent = Intent(context, MainActivity::class.java)
            .putExtra(PILL_TAKEN_VIA_NOTIFICATION_INTENT_KEY, primaryKey)
        val pendingIntent = PendingIntent.getActivity(
            context, requestCode, openMainIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val takePillIntent = Intent(context, ReceiverNotificationAction::class.java)
        takePillIntent.action = "com.quangthe.nhacnho_uongthuoc.ACTION_TAKE_PILL"
        takePillIntent.putExtra(PRIMARY_KEY_INTENT_KEY_STRING, primaryKey)
        takePillIntent.putExtra(NOTIFICATION_ID_INTENT_KEY_STRING, requestCode)

        val takePillPendingIntent = PendingIntent.getBroadcast(
            context, requestCode, takePillIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val description = context.getString(
            R.string.reminder_notification_description,
            if (doseIndex < timesArray.size) timesArray[doseIndex] else name
        )

        val pillReminderNotification = NotificationCompat.Builder(context, Simpill.PILL_REMINDER_CHANNEL)
            .setSmallIcon(R.drawable.pill_bottle_color_2)
            .setContentTitle(name)
            .setContentText(description)
            .setColor(500086)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(
                        context.getString(
                            R.string.reminder_notification_description,
                            name + " (" + (if (doseIndex < timesArray.size) timesArray[doseIndex] else "") + ")"
                        )
                    )
            )
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setLights(Color.RED, 500, 500)
            .setVibrate(AudioHelper.vibratorPattern)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(sticky)
            .setContentIntent(pendingIntent)
            .addAction(R.mipmap.ic_launcher, context.getString(R.string.open), pendingIntent)
            .addAction(R.drawable.pill_bottle_color_1, "Xác nhận đã uống", takePillPendingIntent)
            .build()

        pillNotificationManagerCompat.notify(name, requestCode, pillReminderNotification)
    }

    fun sendStockupNotification(context: Context) {
        val pillNotificationManagerCompat = NotificationManagerCompat.from(context)
        
        if (alarmType == ALARM) {
            context.startActivity(
                Intent(context, PillAlarmDisplay::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
                    .putExtra(PRIMARY_KEY_INTENT_KEY_STRING, primaryKey)
                    .putExtra(NOTIFICATION_ID_INTENT_KEY_STRING, 0)
            )
        }

        val openMainIntent = Intent(context, MainActivity::class.java)
            .putExtra(PILL_TAKEN_VIA_NOTIFICATION_INTENT_KEY, primaryKey)
        val pendingIntent = PendingIntent.getActivity(
            context, primaryKey, openMainIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val sticky = SharedPrefs(context).stickyNotificationsPref

        var pillReminderNotification = NotificationCompat.Builder(context, Simpill.PILL_REMINDER_CHANNEL)
            .setSmallIcon(R.drawable.pill_bottle_color_2)
            .setContentText(context.getString(R.string.reminder_notification_description, name))
            .setColor(500086)
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText(context.getString(R.string.reminder_notification_description, name))
            )
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setOngoing(sticky)
            .setContentIntent(pendingIntent)
            .addAction(R.mipmap.ic_launcher, "Open", pendingIntent)
            .addAction(R.mipmap.ic_launcher, "Dismiss", pendingIntent)
            .build()

        if (taken == PILL_TAKEN_VALUE) {
            pillReminderNotification = NotificationCompat.Builder(context, Simpill.PILL_REMINDER_CHANNEL)
                .setSmallIcon(R.drawable.pill_bottle_color_2)
                .setColor(500086)
                .setStyle(
                    NotificationCompat.BigTextStyle()
                        .bigText(context.getString(R.string.reminder_already_taken_description, name))
                )
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setOngoing(sticky)
                .setContentIntent(pendingIntent)
                .setFullScreenIntent(pendingIntent, true)
                .addAction(R.mipmap.ic_launcher, "Open", pendingIntent)
                .build()
        }

        pillNotificationManagerCompat.notify(name, primaryKey, pillReminderNotification)
    }

    fun deleteActiveNotifications(context: Context, doseIndex: Int) {
        NotificationManagerCompat.from(context).cancel(name, getAlarmRequestCodes()[doseIndex])
    }

    fun setAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        for (index in alarmReminderTimes.indices) {
            val freq = frequency
            val requestCode = getAlarmRequestCodes()[index]
            val reminderTime = alarmReminderTimes[index]

            Log.d("PILL_ALARM", "at " + dateTimeManager.formatLongAsDateTimeString(reminderTime))
            Log.d("PILL_ALARM", "with request code $requestCode")

            val pillAlarmPendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                Intent(context, ReceiverPillAlarm::class.java)
                    .putExtra(PRIMARY_KEY_INTENT_KEY_STRING, primaryKey)
                    .putExtra(NOTIFICATION_ID_INTENT_KEY_STRING, requestCode),
                PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(pillAlarmPendingIntent)

            val interval = AlarmManager.INTERVAL_DAY * Math.max(1, freq)
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP, reminderTime, interval, pillAlarmPendingIntent
            )
        }
        
        val resetRequestCode = primaryKey * 1000 + 999
        val startAutoResetReceiver = Intent(context, ReceiverPillAutoReset::class.java)
        startAutoResetReceiver.putExtra(PRIMARY_KEY_INTENT_KEY_STRING, primaryKey)
        startAutoResetReceiver.putExtra(NOTIFICATION_ID_INTENT_KEY_STRING, resetRequestCode)

        val autoResetPendingIntent = PendingIntent.getBroadcast(
            context,
            resetRequestCode,
            startAutoResetReceiver,
            PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(autoResetPendingIntent)

        val resetInterval = AlarmManager.INTERVAL_DAY * Math.max(1, frequency)
        var resetTime: Long
        if (frequency <= 1) {
            resetTime = dateTimeManager.convertTimeToCurrentDateTimeInMillis("00:01")
        } else {
            val nextMedTime = alarmReminderTimes[0]
            resetTime = DateTime(nextMedTime).withTime(0, 1, 0, 0).millis
            while (resetTime <= System.currentTimeMillis()) {
                resetTime += resetInterval
            }
        }

        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP, resetTime, resetInterval, autoResetPendingIntent
        )
    }

    fun setStockupAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val sDate = calculateStockupDate()
        if (sDate != NULL_DB_ENTRY_STRING) {
            val startPillSupplyReceiver = Intent(context, ReceiverPillSupply::class.java)
            startPillSupplyReceiver.putExtra(PRIMARY_KEY_INTENT_KEY_STRING, primaryKey)

            val pillSupplyPendingIntent = PendingIntent.getBroadcast(
                context,
                primaryKey,
                startPillSupplyReceiver,
                PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(pillSupplyPendingIntent)

            var supplyReminderTime = dateTimeManager.formatDateTimeStringAsLong("$sDate 12:00")

            if (supplyReminderTime < System.currentTimeMillis()) {
                supplyReminderTime += 2629746000L
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

    fun cancelAlarms(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        for (index in alarmReminderTimes.indices) {
            val requestCode = getAlarmRequestCodes()[index]

            val pillAlarmPendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                Intent(context, ReceiverPillAlarm::class.java)
                    .putExtra(PRIMARY_KEY_INTENT_KEY_STRING, primaryKey)
                    .putExtra(NOTIFICATION_ID_INTENT_KEY_STRING, requestCode),
                PendingIntent.FLAG_IMMUTABLE
            )

            alarmManager.cancel(pillAlarmPendingIntent)
        }

        val startPillSupplyReceiver = Intent(context, ReceiverPillSupply::class.java)
        startPillSupplyReceiver.putExtra(PRIMARY_KEY_INTENT_KEY_STRING, primaryKey)

        val pillSupplyPendingIntent = PendingIntent.getBroadcast(
            context,
            primaryKey,
            startPillSupplyReceiver,
            PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.cancel(pillSupplyPendingIntent)
    }
    
    private fun setAlarmReminderTimes() {
        val times = timesArray
        if (times.isEmpty() || times.all { it.isBlank() }) {
            alarmReminderTimes = LongArray(0)
            return
        }
        alarmReminderTimes = LongArray(times.size)
        for (index in times.indices) {
            alarmReminderTimes[index] = dateTimeManager.getNextScheduledTimeMillis(
                startDate ?: NULL_DB_ENTRY_STRING, times[index], frequency
            )
        }
    }

    fun getAlarmReminderTimes(): LongArray = alarmReminderTimes

    fun getAlarmRequestCodes(): IntArray = alarmRequestCodes

    fun setAlarmRequestCodes() {
        alarmRequestCodes = IntArray(getAlarmReminderTimes().size)
        for (index in getAlarmReminderTimes().indices) {
            alarmRequestCodes[index] = primaryKey * 1000 + index
        }
    }

    fun calculateStockupDate(): String {
        return if (stockupDate == null || stockupDate == NULL_DB_ENTRY_STRING) {
            NULL_DB_ENTRY_STRING
        } else {
            val supplyReminderTime = dateTimeManager.formatDateTimeStringAsLong("$stockupDate 12:00")
            if (supplyReminderTime > System.currentTimeMillis()) {
                stockupDate!!
            } else {
                dateTimeManager.addMonthToDateString(stockupDate!!)
            }
        }
    }

    fun getBottleDrawable(context: Context): Drawable? {
        val bottleDrawableId = when (bottleColor) {
            1 -> R.drawable.pill_bottle_color_1
            2 -> R.drawable.pill_bottle_color_2
            3 -> R.drawable.pill_bottle_color_3
            4 -> R.drawable.pill_bottle_color_4
            5 -> R.drawable.pill_bottle_color_5
            6 -> R.drawable.pill_bottle_color_6
            7 -> R.drawable.pill_bottle_color_7
            8 -> R.drawable.pill_bottle_color_8
            9 -> R.drawable.pill_bottle_color_9
            10 -> R.drawable.pill_bottle_color_10
            11 -> R.drawable.pill_bottle_color_11
            12 -> R.drawable.pill_bottle_color_12
            else -> R.drawable.pill_bottle_color_2
        }
        return AppCompatResources.getDrawable(context, bottleDrawableId)
    }

    interface PillListener {
        fun notifyAddedPill(pill: Pill)
        fun notifyDeletedPill(pill: Pill, position: Int)
        fun notifyResetPill(position: Int)
    }

    companion object {
        @JvmField
        val PRIMARY_KEY_INTENT_KEY_STRING = "primaryKey"
        @JvmField
        val NOTIFICATION_ID_INTENT_KEY_STRING = "Notification ID"
        @JvmField
        val PILL_TAKEN_VIA_NOTIFICATION_INTENT_KEY = "Pill Taken From Notification Click"
        @JvmField
        val NULL_DB_ENTRY_STRING = "null"
        @JvmField
        val PILL_TAKEN_VALUE = 1
        @JvmField
        val PILL_NOT_TAKEN_VALUE = 0
        @JvmField
        val DEFAULT_ALARM_URI = Uri.parse("android.resource://com.quangthe.nhacnho_uongthuoc/" + R.raw.eas_alarm)

        @JvmStatic
        fun getDefaultAlarmUri(): Uri = DEFAULT_ALARM_URI

        const val NOTIFICATION = 0
        const val ALARM = 1
        const val CUSTOM_ALARM = 2

        const val MULTIPLE_DAILY = 0
        const val DAILY = 1
        const val EVERY_OTHER_DAY = 2
        const val WEEKLY = 7
    }
}
