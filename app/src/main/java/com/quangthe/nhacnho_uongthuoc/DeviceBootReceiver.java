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
        DatabaseHelper myDatabase = new DatabaseHelper(context);

        if (Objects.equals(intent.getAction(), "android.intent.action.BOOT_COMPLETED")
                && myDatabase.getRowCount() > 0) {
            Pill[] pills = myDatabase.getAllPills();
            for (Pill pill : pills) {
                pill.setAlarm(context);
                pill.setStockupAlarm(context);
            }
            new Toasts(context).showCustomToast(context.getString(R.string.device_restart_toast));
        }
    }
}
