package com.angrycoding.android_sms_proxy;

import android.telephony.SmsManager;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import java.util.ArrayList;
import java.util.Map;

public class MessagingService extends FirebaseMessagingService {

    private void sendSMS(String phone, String text) {
        SmsManager smsManager = SmsManager.getDefault();
        ArrayList<String> parts = smsManager.divideMessage(text);
        smsManager.sendMultipartTextMessage(phone, null, parts, null, null);
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d("TEMP", "HERE");
        Map<String, String> payload = remoteMessage.getData();
        Log.d("TEMP", payload.toString());
        if (payload.size() == 0) return;
        Log.d("TEMP", "From: " + remoteMessage.getFrom());
        Log.d("TEMP", "Message data payload: " + payload);
        String phone = payload.get("phone");
        String text = payload.get("text");
        if (phone != null && text != null && phone.length() != 0 && text.length() != 0) {
            sendSMS(phone, text);
        }
    }

}
