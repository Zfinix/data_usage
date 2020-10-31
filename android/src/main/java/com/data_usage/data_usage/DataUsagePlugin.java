package com.data_usage.data_usage;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AppOpsManager;
import android.app.usage.NetworkStats;
import android.app.usage.NetworkStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.TrafficStats;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.core.app.ActivityCompat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;

/**
 * DataUsagePlugin
 */
public class DataUsagePlugin implements FlutterPlugin, MethodCallHandler, ActivityAware {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private MethodChannel channel;
    private Activity activity;
    private Context context;


    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "data_usage");
        channel.setMethodCallHandler(this);
        context = flutterPluginBinding.getApplicationContext();
    }

    @Override
    public void onMethodCall(@NonNull final MethodCall call, @NonNull final Result result) {
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                switch (call.method) {
                    case "getDataUsage":

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {

                            handleGetDataUsage(result, call);
                        } else {
                            getDataFromOldPhone(result, call);
                        }
                        break;

                    case "getDataUsageOld":

                        getDataFromOldPhone(result, call);
                        break;

                    case "init":
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            handleInit(result);
                        } else {
                            result.error("UNSUPPORTED_VERSION", null, null);
                        }
                        break;
                    default:
                        result.notImplemented();
                }
            }
        });

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void handleInit(Result result) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), context.getPackageName());
        if (mode == AppOpsManager.MODE_ALLOWED) {
            result.success(true);
        } else {
            Intent appIntent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(appIntent);
            handleInit(result);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void handleGetDataUsage(@NonNull Result result, @NonNull MethodCall call) {
        AppOpsManager appOps = (AppOpsManager) context.getSystemService(Context.APP_OPS_SERVICE);
        int mode = appOps.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), context.getPackageName());
        if (mode == AppOpsManager.MODE_ALLOWED) {
            try {

                getDataFromPhone(result, (Boolean) Utils.getValueOrDefault(call.argument("withAppIcon"), false), (Boolean) Utils.getValueOrDefault(call.argument("isWifi"), false));
            } catch (Exception e) {
                result.error("DATA_USAGE_ERROR", e.getMessage(), e.toString());
            }
        } else {
            result.error("DATA_USAGE_ERROR", ".init() was not called", "Please call init first");
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void getDataFromPhone(@NonNull Result result, @NonNull Boolean withIcons, @NonNull Boolean isWifi) {
        NetworkStatsManager networkStatsManager = (NetworkStatsManager) context.getSystemService(Context.NETWORK_STATS_SERVICE);
        TelephonyManager manager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

        @SuppressLint("HardwareIds") String subscriberId = Settings.Secure.getString(activity.getContentResolver(), Settings.Secure.ANDROID_ID);

        final PackageManager pm = context.getPackageManager();
        //get a list of installed apps.
        ArrayList<HashMap<String, Object>> packagesData = new ArrayList<>();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);


        for (ApplicationInfo _package : packages) {

            NetworkStats networkStats;
            if (isWifi) {
                networkStats = networkStatsManager.queryDetailsForUid(ConnectivityManager.TYPE_WIFI, subscriberId, 0, System.currentTimeMillis(), _package.uid);
            } else {
                networkStats = networkStatsManager.queryDetailsForUid(ConnectivityManager.TYPE_MOBILE, subscriberId, 0, System.currentTimeMillis(), _package.uid);
            }
            long rxBytes = 0L;
            long txBytes = 0L;
            NetworkStats.Bucket bucket = new NetworkStats.Bucket();
            while (networkStats.hasNextBucket()) {
                networkStats.getNextBucket(bucket);
                rxBytes += bucket.getRxBytes();
                txBytes += bucket.getTxBytes();
            }
            networkStats.close();
            HashMap<String, Object> data = new HashMap<>();
            data.put("app_name", _package.loadLabel(pm).toString());
            data.put("package_name", _package.packageName);
            if (withIcons) {
                try {
                    Drawable icon = pm.getApplicationIcon(_package.packageName);
                    data.put("app_icon", Utils.drawableToBase64(icon));
                } catch (PackageManager.NameNotFoundException e) {

                }

            }
            data.put("received", rxBytes);
            data.put("sent", txBytes);

            packagesData.add(data);

        }

        result.success(packagesData);
    }


    private void getDataFromOldPhone(@NonNull Result result, @NonNull MethodCall call) {
        Boolean withIcons = (Boolean) Utils.getValueOrDefault(call.argument("withAppIcon"), false);
        final PackageManager pm = context.getPackageManager();
        ArrayList<HashMap<String, Object>> packagesData = new ArrayList<>();
        List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);


        try {

            for (ApplicationInfo _package : packages) {

                long rxBytes = TrafficStats.getUidRxBytes(_package.uid);
                long txBytes = TrafficStats.getUidTxBytes(_package.uid);
                HashMap<String, Object> data = new HashMap<>();
                data.put("app_name", _package.loadLabel(pm).toString());
                data.put("package_name", _package.packageName);
                if (withIcons) {
                    Drawable icon = pm.getApplicationIcon(pm.getApplicationInfo(_package.processName, PackageManager.GET_META_DATA));
                    data.put("app_icon", Utils.drawableToBase64(icon));
                }
                data.put("received", rxBytes);
                data.put("sent", txBytes);

                packagesData.add(data);

            }

            result.success(packagesData);
        } catch (Exception e) {
            result.error("DATA_USAGE_ERROR", e.getMessage(), e.toString());
        }

    }



    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }

    @Override
    public void onDetachedFromActivity() {
        //TODO("Not yet implemented")
    }

    @Override
    public void onReattachedToActivityForConfigChanges(ActivityPluginBinding binding) {
        //TODO("Not yet implemented")
    }

    @Override
    public void onAttachedToActivity(ActivityPluginBinding binding) {
        activity = binding.getActivity();
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {
        // TODO("Not yet implemented")
    }

}

