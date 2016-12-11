package ca.justinrichard.link;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * Created by Justin on 12/10/2016.
 */

public class MyBroadcastReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent)
    {
        Intent myIntent = new Intent(context, MyFirebaseInstanceIDService.class);
        context.startService(myIntent);
        myIntent = new Intent(context, MyFirebaseMessagingService.class);
        context.startService(myIntent);
    }
}