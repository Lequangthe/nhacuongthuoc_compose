/* (C) 2022 */
package com.quangthe.nhacnho_uongthuoc;

import com.quangthe.nhacnho_uongthuoc.R;
import android.annotation.SuppressLint;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import java.util.Objects;

public class DeviceBootReceiver extends BroadcastReceiver {
    @SuppressLint("ShortAlarm")
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Objects.equals(intent.getAction(), "android.intent.action.BOOT_COMPLETED")) {
            new Thread(() -> {
                java.util.List<Pill> pills = AppDatabase.Companion.getDatabase(context).pillDao().getAllPillsNonSuspend();
                if (!pills.isEmpty()) {
                    for (Pill pill : pills) {
                        pill.setAlarm(context);
                        pill.setStockupAlarm(context);
                    }
                    new android.os.Handler(android.os.Looper.getMainLooper()).post(() -> {
                        new Toasts(context).showCustomToast(context.getString(R.string.device_restart_toast));
                    });
                }
            }).start();
        }
    }
}
