/* (C) 2022 */
package com.quangthe.nhacnho_uongthuoc;

import static com.quangthe.nhacnho_uongthuoc.Pill.PRIMARY_KEY_INTENT_KEY_STRING;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ReceiverPillAutoReset extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        int pk = intent.getIntExtra(PRIMARY_KEY_INTENT_KEY_STRING, -1);
        new Thread(() -> {
            PillDao pillDao = AppDatabase.Companion.getDatabase(context).pillDao();
            Pill pill = pillDao.getPillSync(pk);
            if (pill != null) {
                pill.autoResetPill(context);
                pillDao.updatePillSync(pill);
            }
        }).start();
    }
}
