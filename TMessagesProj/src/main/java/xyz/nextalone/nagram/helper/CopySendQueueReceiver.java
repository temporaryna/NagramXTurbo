package xyz.nextalone.nagram.helper;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class CopySendQueueReceiver extends BroadcastReceiver {

    public static final String ACTION_CANCEL = "xyz.nextalone.nagram.COPY_SEND_CANCEL";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION_CANCEL.equals(intent.getAction())) {
            return;
        }
        int account = intent.getIntExtra("account", 0);
        CopySendQueue.getInstance(account).cancelJob();
    }
}
