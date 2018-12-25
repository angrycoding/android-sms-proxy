package com.angrycoding.android_sms_proxy;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.telephony.SmsManager;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Map;

public class MessagingService extends FirebaseMessagingService {

    SmsManager smsManager = null;
    RequestQueue requestQueue = null;

    private void sendStatusUpdate(String url) {
        if (url == null) return;
        if (requestQueue == null) requestQueue = Volley.newRequestQueue(this);
        requestQueue.add(new StringRequest(url, null, null));
    }

    private void sendSMS(String text, String number, final String successURL, final String failURL) {
        if (smsManager == null) smsManager = SmsManager.getDefault();
        ArrayList<String> parts = smsManager.divideMessage(text);
        ArrayList<PendingIntent> sentIntents = null;

        if (successURL != null || failURL != null) {
            final int partsSize = parts.size();


            String intentName = successURL;
            if (intentName == null) intentName = failURL;

            sentIntents = new ArrayList<PendingIntent>(partsSize);
            for (int c = 0; c < partsSize; c++) {
                Intent intent = new Intent(intentName);
                sentIntents.add(PendingIntent.getBroadcast(this, 0, intent, PendingIntent.FLAG_IMMUTABLE));
            }

            registerReceiver(new BroadcastReceiver() {


                private int pSize = partsSize;
                private String _successURL = successURL;
                private String _failURL = failURL;

                @Override
                public void onReceive(Context context, Intent intent) {


                    if (getResultCode() != Activity.RESULT_OK) {
                        context.unregisterReceiver(this);
                        Log.d("TEMP", "NOT_OK");
                        sendStatusUpdate(_failURL);
                    }

                    pSize--;

                    if (pSize <= 0) {
                        Log.d("TEMP", "OK");
                        context.unregisterReceiver(this);
                        sendStatusUpdate(_successURL);
                    }



                }
            }, new IntentFilter(intentName));

        }

        smsManager.sendMultipartTextMessage(number, null, parts, sentIntents, null);
    }

    private void sendSMS(String text, JSONArray numbers) {
        for (int c = 0; c < numbers.length(); c++) try {
            JSONObject numberObj = numbers.getJSONObject(c);
            String number = numberObj.getString("number").trim();
            String successURL = numberObj.has("success") ? numberObj.getString("success") : null;
            String failURL = numberObj.has("fail") ? numberObj.getString("fail") : null;
            if (number.length() != 0) sendSMS(text, number, successURL, failURL);
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
