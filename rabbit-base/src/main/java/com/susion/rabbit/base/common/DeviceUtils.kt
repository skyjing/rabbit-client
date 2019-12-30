package com.susion.rabbit.base.common

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.text.TextUtils
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.*
import java.util.*


/**
 * susionwang at 2019-09-23
 */
object DeviceUtils {

    private val TAG = "DeviceUtils"

    /**
     * get version of emui
     */
    fun getEmuiVersion(): Double {
        try {
            val emuiVersion =
                com.susion.rabbit.base.common.DeviceUtils.getSystemProperty("ro.build.version.emui")
            val version = emuiVersion!!.substring(emuiVersion.indexOf("_") + 1)
            return java.lang.Double.parseDouble(version)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return 4.0
    }

    /**
     * get xiaomi version of emui
     */
    fun getMiuiVersion(): Int {
        val version =
            com.susion.rabbit.base.common.DeviceUtils.getSystemProperty("ro.miui.ui.version.name")
        if (version != null) {
            try {
                return Integer.parseInt(version.substring(1))
            } catch (e: Exception) {
                Log.e(com.susion.rabbit.base.common.DeviceUtils.TAG, "get miui version code error, version : $version")
            }

        }
        return -1
    }

    /**
     * get system property for "ro.build.version.emui"
     */
    fun getSystemProperty(propName: String): String? {
        val line: String
        var input: BufferedReader? = null
        try {
            val p = Runtime.getRuntime().exec("getprop $propName")
            input = BufferedReader(InputStreamReader(p.inputStream), 1024)
            line = input.readLine()
            input.close()
        } catch (ex: IOException) {
            Log.e(com.susion.rabbit.base.common.DeviceUtils.TAG, "Unable to read sysprop $propName", ex)
            return null
        } finally {
            if (input != null) {
                try {
                    input.close()
                } catch (e: IOException) {
                    Log.e(com.susion.rabbit.base.common.DeviceUtils.TAG, "Exception while closing InputStream", e)
                }

            }
        }
        return line
    }

    /**
     * checking if is huawei rom
     */
    fun checkIsHuaweiRom(): Boolean {
        return Build.MANUFACTURER.contains("HUAWEI")
    }

    /**
     * checking if is miui rom
     */
    fun checkIsMiuiRom(): Boolean {
        return !TextUtils.isEmpty(com.susion.rabbit.base.common.DeviceUtils.getSystemProperty("ro.miui.ui.version.name"))
    }

    /**
     * checking if is meizu rom
     */
    fun checkIsMeizuRom(): Boolean {
        val meizuFlymeOSFlag =
            com.susion.rabbit.base.common.DeviceUtils.getSystemProperty("ro.build.display.id")
        return if (TextUtils.isEmpty(meizuFlymeOSFlag)) {
            false
        } else meizuFlymeOSFlag!!.contains("flyme") || meizuFlymeOSFlag.toLowerCase().contains("flyme")
    }

    /**
     * checking if is 360 rom
     */
    fun checkIs360Rom(): Boolean {
        return Build.MANUFACTURER.contains("QiKU") || Build.MANUFACTURER.contains("360")
    }

    /**
     * checking if is oppo rom
     */
    fun checkIsOppoRom(): Boolean {
        return Build.MANUFACTURER.contains("OPPO") || Build.MANUFACTURER.contains("oppo")
    }

    fun getDeviceName(): String {
        val manufacturer = if (Build.MANUFACTURER == null) "" else Build.MANUFACTURER
        val model = if (Build.MODEL == null) "" else Build.MODEL
        return if (model.startsWith(manufacturer)) {
            com.susion.rabbit.base.common.DeviceUtils.capitalize(model)
        } else {
            com.susion.rabbit.base.common.DeviceUtils.capitalize(manufacturer) + " " + model
        }
    }

    private fun capitalize(s: String): String {
        if (TextUtils.isEmpty(s) || s.trim { it <= ' ' }.length == 0) {
            return ""
        }

        val first = s[0]
        return if (Character.isUpperCase(first)) {
            s
        } else {
            Character.toUpperCase(first) + s.substring(1)
        }
    }

    private var uuid: UUID? = null
    private val PREFS_DEVICE_ID = "device_id"
    private val PREFS_FILE = "pre_device.xml"

    fun getDeviceId(context: Context?): String {
        if (context == null) {
            return ""
        }
        if (com.susion.rabbit.base.common.DeviceUtils.uuid == null) {
            synchronized(com.susion.rabbit.base.common.DeviceUtils::class.java) {
                if (com.susion.rabbit.base.common.DeviceUtils.uuid == null) {
                    val prefs = context.getSharedPreferences(com.susion.rabbit.base.common.DeviceUtils.PREFS_FILE, 0)
                    val id = prefs.getString(com.susion.rabbit.base.common.DeviceUtils.PREFS_DEVICE_ID, null)
                    if (id != null) {
                        // Use the ids previously computed and stored in the prefs file
                        com.susion.rabbit.base.common.DeviceUtils.uuid = UUID.fromString(id)
                    } else {
                        val androidId =
                            com.susion.rabbit.base.common.DeviceUtils.getAndroidId(context)
                        // Use the Android ID unless it's broken, in which case fallback on deviceId,
                        // unless it's not isEmpty, then fallback on a random number which we store
                        // to a prefs file
                        try {
                            if (!android.text.TextUtils.isEmpty(androidId) && "9774d56d682e549c" != androidId) {
                                com.susion.rabbit.base.common.DeviceUtils.uuid =
                                    UUID.nameUUIDFromBytes(androidId.toByteArray(charset("utf8")))
                            } else {
                                val deviceId =
                                    com.susion.rabbit.base.common.DeviceUtils.getIMEIId(context)
                                com.susion.rabbit.base.common.DeviceUtils.uuid = if (deviceId != null && !android.text.TextUtils.equals(
                                        deviceId,
                                        "unknow"
                                    )
                                )
                                    UUID.nameUUIDFromBytes(deviceId!!.toByteArray(charset("utf8")))
                                else
                                    UUID.randomUUID()
                            }
                        } catch (e: UnsupportedEncodingException) {
                            e.printStackTrace()
                        }

                        prefs.edit().putString(com.susion.rabbit.base.common.DeviceUtils.PREFS_DEVICE_ID, com.susion.rabbit.base.common.DeviceUtils.uuid.toString()).apply()
                    }
                }
            }
        }
        return com.susion.rabbit.base.common.DeviceUtils.uuid.toString()
    }

    @SuppressLint("MissingPermission")
    fun getIMEIId(context: Context?): String {
        if (context == null) return "unknow"
        var deviceId = ""
        try {
            if (ContextCompat.checkSelfPermission(
                    context,
                    "android.permission.READ_PHONE_STATE"
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val telephony =
                    context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
                if (telephony != null) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        deviceId = telephony.imei
                    } else {
                        deviceId = telephony.deviceId
                    }
                }

            }
        } catch (var3: Exception) {
            var3.printStackTrace()
        }

        return deviceId
    }

    /**
     * 获取设备androidId
     *
     * @param context
     * @return
     */
    fun getAndroidId(context: Context): String {
        return Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
    }


    fun getAppVersionCode(context: Context): String? {
        val manager = context.packageManager
        var name: String? = null
        try {
            val info = manager.getPackageInfo(context.packageName, 0)
            name = info.versionName
        } catch (e: PackageManager.NameNotFoundException) {
            e.printStackTrace()
        }

        return name
    }

    fun getMemorySize(context: Context): String {
        try {
            //1, 读取Android设备的内存信息文件内容
            val fileReader = FileReader("/proc/meminfo")
            BufferedReader(fileReader).use {
                val size = it.readLine().replace("[^0-9.,]+".toRegex(), "").toLongOrNull() ?: 0
                return com.susion.rabbit.base.common.RabbitUiUtils.formatFileSize(size * 1024)
            }
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return "0"
    }

    /**
     * Return whether device is rooted.
     *
     * @return `true`: yes<br></br>`false`: no
     */
    fun isDeviceRooted(): Boolean {
        val su = "su"
        val locations = arrayOf(
            "/system/bin/",
            "/system/xbin/",
            "/sbin/",
            "/system/sd/xbin/",
            "/system/bin/failsafe/",
            "/data/local/xbin/",
            "/data/local/bin/",
            "/data/local/",
            "/system/sbin/",
            "/usr/bin/",
            "/vendor/bin/"
        )
        for (location in locations) {
            if (File(location + su).exists()) {
                return true
            }
        }
        return false
    }

}