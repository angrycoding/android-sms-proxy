package com.angrycoding.android_sms_proxy;

import android.telephony.SmsManager;

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
        Map<String, String> payload = remoteMessage.getData();
        if (payload.containsKey("phone") && payload.containsKey("text")) {
            String phone = payload.get("phone").trim(), text = payload.get("text").trim();
            if (phone.length() != 0 && text.length() != 0) sendSMS(phone, text);
        }
    }

}
