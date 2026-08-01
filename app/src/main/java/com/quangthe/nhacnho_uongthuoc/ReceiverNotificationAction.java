/* (C) 2026 */
package com.quangthe.nhacnho_uongthuoc;

import static com.quangthe.nhacnho_uongthuoc.Pill.PRIMARY_KEY_INTENT_KEY_STRING;
import static com.quangthe.nhacnho_uongthuoc.Pill.NOTIFICATION_ID_INTENT_KEY_STRING;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import androidx.core.app.NotificationManagerCompat;

public class ReceiverNotificationAction extends BroadcastReceiver {
    public static final String ACTION_TAKE_PILL = "com.quangthe.nhacnho_uongthuoc.ACTION_TAKE_PILL";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_TAKE_PILL.equals(intent.getAction())) {
            int pk = intent.getIntExtra(PRIMARY_KEY_INTENT_KEY_STRING, -1);
            int requestCode = intent.getIntExtra(NOTIFICATION_ID_INTENT_KEY_STRING, -1);
            int doseIndex = requestCode % 1000;

            Log.d("PillNotifyAction", "Action TAKE_PILL received for PK: " + pk + ", Dose: " + doseIndex);

            new Thread(() -> {
                PillDao pillDao = AppDatabase.Companion.getDatabase(context).pillDao();
                Pill pill = pillDao.getPillSync(pk);
                if (pill != null) {
                    pill.takePill(context, doseIndex);
                    pillDao.updatePillSync(pill);
                    pill.deleteActiveNotifications(context, doseIndex);
                    
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        new Toasts(context).showCustomToast("Đã xác nhận uống " + pill.getName() + " cữ " + pill.getTimesArray()[doseIndex]);
                    });
                }
            }).start();
        }
    }
}
