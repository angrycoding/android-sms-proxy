package com.angrycoding.android_sms_proxy;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.provider.Settings.Secure;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

public class MainActivity extends WebViewActivity {

    private String deviceId;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        deviceId = Secure.getString(getContentResolver(), Secure.ANDROID_ID);
    }

    @JavascriptInterface
    public String getPhoneToken() {
        return deviceId;
    }

    @JavascriptInterface
    public boolean getIsSubscribed() {
        return preferences.getBoolean("isSubscribed", false);
    }

    @JavascriptInterface
    public void doSubscribe() {
        try {
            FirebaseMessaging.getInstance().subscribeToTopic(deviceId).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) preferences.edit().putBoolean("isSubscribed", true).apply();
                    dispatchEvent("onSubscriptionChange");
                }
            });
        } catch (Exception e) {
            dispatchEvent("onSubscriptionChange");
        }
    }

    @JavascriptInterface
    public void doUnsubscribe() {
        try {
            FirebaseMessaging.getInstance().unsubscribeFromTopic(deviceId).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) preferences.edit().putBoolean("isSubscribed", false).apply();
                    dispatchEvent("onSubscriptionChange");
                }
            });
        } catch (Exception e) {
            dispatchEvent("onSubscriptionChange");
        }
    }

}
