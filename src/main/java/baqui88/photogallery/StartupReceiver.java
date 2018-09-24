package baqui88.photogallery;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

// Just like services and activities, broadcast receivers must be registered with the system to do anything useful.
// If the receiver is not registered with the system, the system will not send any intents its way
// and, in turn, the receiver’s onReceive(…) will not get executed as desired.

// A broadcast receiver is a component that receives intents, just like a service or an activity. When an
// intent is issued to StartupReceiver, its onReceive(…) method will be called
public class StartupReceiver extends BroadcastReceiver {
    private static final String TAG = "StartupReceiver";

    // With <action android:name="android.intent.action.BOOT_COMPLETED"/>
    // system will start polling since your android is booted, we don't need have application on
    // The next work is to ensure that notification will be only shown when application off
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "Received broadcast intent: " + intent.getAction());

        boolean isOn = QueryPreferences.isAlarmOn(context);
        PollService.setServiceAlarm(context, isOn);
    }
}
