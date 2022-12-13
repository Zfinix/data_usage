package com.chizi.data_usage

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.TrafficStats
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.provider.Settings
import android.telephony.TelephonyManager
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.chizi.data_usage.Utils.drawableToBase64
import com.chizi.data_usage.Utils.getValueOrDefault
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.FlutterPlugin.FlutterPluginBinding
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.PluginRegistry
import java.util.*

/**
 * DataUsagePlugin
 */
class DataUsagePlugin : FlutterPlugin, MethodCallHandler, ActivityAware,
        PluginRegistry.RequestPermissionsResultListener {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private var channel: MethodChannel? = null
    private var activity: Activity? = null
    private var context: Context? = null
    private val myPermissionCode = 72
    private var permissionGranted: Boolean = false
    private var _result: MethodChannel.Result? = null


    override fun onAttachedToEngine(flutterPluginBinding: FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "data_usage")
        channel!!.setMethodCallHandler(this)
        context = flutterPluginBinding.applicationContext


    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        Handler(Looper.getMainLooper()).post {

            permissionGranted = ContextCompat.checkSelfPermission(context!!,
                    Manifest.permission.READ_PHONE_STATE) ==
                    PackageManager.PERMISSION_GRANTED

            when (call.method) {
                "getDataUsage" -> {
                    if (permissionGranted) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                            handleGetDataUsage(result, call)
                        else getDataFromOldPhone(result, call)

                    } else {
                        result.error("PERMISSION_NOT_GRANTED", null, null)
                    }
                }
                "getDataUsageOld" -> if (permissionGranted) {
                    getDataFromOldPhone(result, call)
                } else {
                    result.error("PERMISSION_NOT_GRANTED", null, null)
                }
                "init" -> if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {

                    if (!permissionGranted) {

                        _result = result

                        requestPermission()

                    } else {
                        handleInit(result)

                    }

                } else {
                    result.error("UNSUPPORTED_VERSION", null, null)
                }
                else -> result.notImplemented()
            }
        }
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(activity!!,
                arrayOf(Manifest.permission.READ_PHONE_STATE), myPermissionCode)

    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun handleInit(result: MethodChannel.Result) {

        if (isAccessGranted()) {
            result.success(true)
        } else {
            val appIntent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            appIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context!!.startActivity(appIntent)
            handleInit(result)
        }
        _result = null
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun handleGetDataUsage(result: MethodChannel.Result, call: MethodCall) {
        if (isAccessGranted()) {
            try {
                getDataFromPhone(result, getValueOrDefault(call.argument<Boolean>("withAppIcon"), false), getValueOrDefault(call.argument<Boolean>("isWifi"), false))
            } catch (e: Exception) {
                result.error("DATA_USAGE_ERROR", e.message, e.toString())
            }
        } else {
            result.error("DATA_USAGE_ERROR", ".init() was not called", "Please call init first")
        }
    }
    @RequiresApi(Build.VERSION_CODES.KITKAT)
    private fun isAccessGranted(): Boolean {
        return try {
            val packageManager: PackageManager = context!!.packageManager
            val applicationInfo: ApplicationInfo = packageManager.getApplicationInfo(context!!.packageName, 0)
            val appOpsManager = context!!.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            var mode = 0
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT) {
                mode = appOpsManager.checkOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                        applicationInfo.uid, applicationInfo.packageName)
            }
            mode == AppOpsManager.MODE_ALLOWED
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private fun getDataFromPhone(result: MethodChannel.Result, withIcons: Boolean, isWifi: Boolean) {
        val networkStatsManager = context!!.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
        val manager = context!!.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        @SuppressLint("HardwareIds") val subscriberId = Settings.Secure.getString(activity!!.contentResolver, Settings.Secure.ANDROID_ID)
        val pm = context!!.packageManager
        //get a list of installed apps.
        val packagesData = ArrayList<HashMap<String, Any?>>()
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        for (_package in packages) {
            var networkStats: NetworkStats = if (isWifi) {
                networkStatsManager.queryDetailsForUid(ConnectivityManager.TYPE_WIFI, subscriberId, 0, System.currentTimeMillis(), _package.uid)
            } else {
                networkStatsManager.queryDetailsForUid(ConnectivityManager.TYPE_MOBILE, subscriberId, 0, System.currentTimeMillis(), _package.uid)
            }
            var rxBytes = 0L
            var txBytes = 0L
            val bucket = NetworkStats.Bucket()
            while (networkStats.hasNextBucket()) {
                networkStats.getNextBucket(bucket)
                rxBytes += bucket.rxBytes
                txBytes += bucket.txBytes
            }
            networkStats.close()
            val data = HashMap<String, Any?>()
            data["app_name"] = _package.loadLabel(pm).toString()
            data["package_name"] = _package.packageName
            if (withIcons) {
                try {
                    val icon = pm.getApplicationIcon(_package.packageName)
                    data["app_icon"] = drawableToBase64(icon)
                } catch (e: PackageManager.NameNotFoundException) {
                }
            }
            data["received"] = rxBytes
            data["sent"] = txBytes
            packagesData.add(data)
        }
        result.success(packagesData)
    }

    private fun getDataFromOldPhone(result: MethodChannel.Result, call: MethodCall) {
        val withIcons = getValueOrDefault(call.argument<Any>("withAppIcon"), false) as Boolean
        val pm = context!!.packageManager
        val packagesData = ArrayList<HashMap<String, Any?>>()
        val packages = pm.getInstalledApplications(PackageManager.GET_META_DATA)
        try {
            for (_package in packages) {
                val rxBytes = TrafficStats.getUidRxBytes(_package.uid)
                val txBytes = TrafficStats.getUidTxBytes(_package.uid)
                val data = HashMap<String, Any?>()
                data["app_name"] = _package.loadLabel(pm).toString()
                data["package_name"] = _package.packageName
                if (withIcons) {
                    val icon = pm.getApplicationIcon(pm.getApplicationInfo(_package.processName, PackageManager.GET_META_DATA))
                    data["app_icon"] = drawableToBase64(icon)
                }
                data["received"] = rxBytes
                data["sent"] = txBytes
                packagesData.add(data)
            }
            result.success(packagesData)
        } catch (e: Exception) {
            result.error("DATA_USAGE_ERROR", e.message, e.toString())
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPluginBinding) {
        channel!!.setMethodCallHandler(null)
    }

    override fun onDetachedFromActivity() {
        activity = null
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        activity = binding.activity
        binding.addRequestPermissionsResultListener(this)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        activity = binding.activity
        binding.addRequestPermissionsResultListener(this)
    }

    override fun onDetachedFromActivityForConfigChanges() {
        activity = null
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<out String>,
                                            grantResults: IntArray): Boolean {

        when (requestCode) {
            myPermissionCode -> {
                if (null != grantResults) {
                    permissionGranted = grantResults.isNotEmpty() &&
                            grantResults[0] == PackageManager.PERMISSION_GRANTED
                }

                if (permissionGranted && _result != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        handleInit(_result!!)
                    }
                }
                // only return true if handling the request code
                return true
            }
        }
        return false
    }
}