package com.angrycoding.android_sms_proxy;

import android.telephony.SmsManager;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;

public class MessagingService extends FirebaseMessagingService {

    SmsManager smsManager = null;

    private void sendSMS(String text, String number) {
        if (smsManager == null) smsManager = SmsManager.getDefault();
        ArrayList<String> parts = smsManager.divideMessage(text);
        smsManager.sendMultipartTextMessage(number, null, parts, null, null);
    }

    private void sendSMS(String text, JSONArray numbers) {
        for (int c = 0; c < numbers.length(); c++) try {
            String number = numbers.getString(c).trim();
            if (number.length() != 0) sendSMS(text, number);
        } catch (Exception e) {}
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Map<String, String> payload = remoteMessage.getData();
        if (!payload.containsKey("json")) return;
        try {
            String json = payload.get("json");
            JSONObject request = new JSONObject(json);
            if (!request.has("text")) return;
            if (!request.has("numbers")) return;
            String text = request.getString("text").trim();
            JSONArray numbers = request.getJSONArray("numbers");
            if (text.length() != 0 && numbers.length() != 0) sendSMS(text, numbers);
        } catch (Exception e) {}
    }

}
