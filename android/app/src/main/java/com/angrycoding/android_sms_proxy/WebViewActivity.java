package com.angrycoding.android_sms_proxy;

import android.app.ActionBar;
import android.app.Activity;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.webkit.ConsoleMessage;
import android.webkit.JavascriptInterface;
import android.webkit.MimeTypeMap;
import android.webkit.PermissionRequest;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.ArrayList;

public class WebViewActivity extends Activity {

    protected WebView webView;

    private WebViewClient webViewClient = new WebViewClient() {
        @Override
        public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
            Uri requestURL = request.getUrl();
            if (requestURL.getHost().equals("assets")) {
                String requestPath = requestURL.getPath();
                String extension = MimeTypeMap.getFileExtensionFromUrl(requestPath);
                String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
                if (extension.equals("js")) mime = "text/javascript";
                try {
                    return new WebResourceResponse(mime, "UTF-8", getAssets().open(requestPath.substring(1)));
                } catch (Exception e) {
                }
            }
            return super.shouldInterceptRequest(view, request);
        }
    };

    @JavascriptInterface
    public void readyReady() {
        runOnUiThread(new Runnable(){
            public void run() {
                webView.setVisibility(View.VISIBLE);
                ActionBar actionBar = getActionBar();
                if (actionBar != null) actionBar.show();
            }
        });
    }

    @JavascriptInterface
    public boolean checkPermissions() {
        boolean result = true;
        ArrayList<String> permissions = new ArrayList<>();
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_PERMISSIONS);
            if (info.requestedPermissions != null) {
                for (String permission : info.requestedPermissions) {
                    if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                        result = false;
                        break;
                    }
                }
            }
        } catch (Exception e) {}
        return result;
    }

    @JavascriptInterface
    public void grantPermissions() {
        ArrayList<String> permissions = new ArrayList<>();
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_PERMISSIONS);
            if (info.requestedPermissions != null) {
                for (String permission : info.requestedPermissions) {
                    if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
                        permissions.add(permission);
                    }
                }
            }
        } catch (Exception e) {}

        if (permissions.size() > 0) {
            String[] stockArr = new String[permissions.size()];
            stockArr = permissions.toArray(stockArr);
            requestPermissions(stockArr, 1);
        } else onRequestPermissionsResult(0, null, null);

    }

    protected void dispatchEvent(String eventType) {
        webView.evaluateJavascript("document.dispatchEvent(new Event('" + eventType + "'));", null);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        dispatchEvent("onPermissionChange");
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getActionBar();
        if (actionBar != null) actionBar.hide();

        String loadUrl = null;

        try {
            ActivityInfo activityInfo = getPackageManager().getActivityInfo(this.getComponentName(), PackageManager.GET_META_DATA);
            loadUrl = activityInfo.metaData.getString("loadUrl");
        } catch (Exception e) {}

        webView = new WebView(this);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setAllowContentAccess(true);
        settings.setAllowFileAccessFromFileURLs(true);
        settings.setAllowFileAccess(true);
        settings.setAllowUniversalAccessFromFileURLs(true);
        settings.setStandardFontFamily("Roboto-Light");

        webView.setVisibility(View.INVISIBLE);
        webView.addJavascriptInterface(this, "android");
        webView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        webView.setWebViewClient(webViewClient);
        webView.setWebChromeClient(new WebChromeClient() {

            @Override
            public void onPermissionRequest(final PermissionRequest request) {
                Log.d("TEMP", "onPermissionRequest");
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Log.d("TEMP", request.getOrigin().toString());
                        if(request.getOrigin().toString().contains("assets")) {
                            Log.d("TEMP", "GRANTED");
                            request.grant(request.getResources());
                        } else {
                            Log.d("TEMP", "DENIED");
                            request.deny();
                        }
                    }
                });
            }

            @Override
            public boolean onConsoleMessage(ConsoleMessage consoleMessage) {
                Log.d("TEMP", consoleMessage.message());
                return true;
            }
        });

        webView.loadUrl(loadUrl);
        this.setContentView(webView);
    }

}