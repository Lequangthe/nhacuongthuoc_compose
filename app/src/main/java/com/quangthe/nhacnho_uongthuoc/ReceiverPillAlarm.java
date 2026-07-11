/* (C) 2022 */
package com.quangthe.nhacnho_uongthuoc;

import static com.quangthe.nhacnho_uongthuoc.Pill.PRIMARY_KEY_INTENT_KEY_STRING;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class ReceiverPillAlarm extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        DatabaseHelper databaseHelper = new DatabaseHelper(context);
        Pill pill = databaseHelper.getPill(intent.getIntExtra(PRIMARY_KEY_INTENT_KEY_STRING, -1));
        
        int requestCode = intent.getIntExtra(Pill.NOTIFICATION_ID_INTENT_KEY_STRING, -1);
        int doseIndex = requestCode % 1000;
        
        pill.sendPillNotification(context, doseIndex);
        pill.setAlarm(context);
    }
}
