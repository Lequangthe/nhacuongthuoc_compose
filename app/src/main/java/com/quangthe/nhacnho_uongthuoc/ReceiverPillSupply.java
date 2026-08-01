/* (C) 2022 */
package com.quangthe.nhacnho_uongthuoc;

import static com.quangthe.nhacnho_uongthuoc.Pill.PRIMARY_KEY_INTENT_KEY_STRING;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ReceiverPillSupply extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int pk = intent.getIntExtra(PRIMARY_KEY_INTENT_KEY_STRING, -1);
        new Thread(() -> {
            Pill pill = AppDatabase.Companion.getDatabase(context).pillDao().getPillSync(pk);
            if (pill != null) {
                pill.sendStockupNotification(context);
                pill.setStockupAlarm(context);
            }
        }).start();
    }
}
