/*
 * Tencent is pleased to support the open source community by making Tinker available.
 *
 * Copyright (C) 2016 THL A29 Limited, a Tencent company. All rights reserved.
 *
 * Licensed under the BSD 3-Clause License (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 *
 * https://opensource.org/licenses/BSD-3-Clause
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" basis, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tinker.deeson.mytinkerdemo;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.StatFs;
import android.text.TextUtils;
import android.util.Log;
import com.tencent.tinker.loader.shareutil.ShareConstants;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.util.UUID;

class Utils {

    /**
     * the error code define by myself
     * should after {@code ShareConstants.ERROR_PATCH_INSERVICE
     */
    private static final int ERROR_PATCH_GOOGLEPLAY_CHANNEL = -5;
    private static final int ERROR_PATCH_ROM_SPACE = -6;
    private static final int ERROR_PATCH_MEMORY_LIMIT = -7;
    public static final int ERROR_PATCH_ALREADY_APPLY = -8;
    public static final int ERROR_PATCH_CRASH_LIMIT = -9;
    public static final int ERROR_PATCH_RETRY_COUNT_LIMIT = -10;
    public static final int ERROR_PATCH_CONDITION_NOT_SATISFIED = -11;

    public static final String PLATFORM = "platform";

    private static final int MIN_MEMORY_HEAP_SIZE = 45;

    private static boolean background = false;

    public static boolean isGooglePlay() {
        return false;
    }

    public static boolean isBackground() {
        return background;
    }

    public static void setBackground(boolean back) {
        background = back;
    }

    public static int checkForPatchRecover(long roomSize, int maxMemory) {
        if (Utils.isGooglePlay()) {
            return Utils.ERROR_PATCH_GOOGLEPLAY_CHANNEL;
        }
        if (maxMemory < MIN_MEMORY_HEAP_SIZE) {
            return Utils.ERROR_PATCH_MEMORY_LIMIT;
        }
        //or you can mention user to clean their rom space!
        if (!checkRomSpaceEnough(roomSize)) {
            return Utils.ERROR_PATCH_ROM_SPACE;
        }

        return ShareConstants.ERROR_PATCH_OK;
    }

    public static boolean isXposedExists(Throwable thr) {
        StackTraceElement[] stackTraces = thr.getStackTrace();
        for (StackTraceElement stackTrace : stackTraces) {
            final String clazzName = stackTrace.getClassName();
            if (clazzName != null && clazzName.contains("de.robv.android.xposed.XposedBridge")) {
                return true;
            }
        }
        return false;
    }

    @Deprecated
    public static boolean checkRomSpaceEnough(long limitSize) {
        long allSize;
        long availableSize = 0;
        try {
            File data = Environment.getDataDirectory();
            StatFs sf = new StatFs(data.getPath());
            availableSize = (long) sf.getAvailableBlocks() * (long) sf.getBlockSize();
            allSize = (long) sf.getBlockCount() * (long) sf.getBlockSize();
        } catch (Exception e) {
            allSize = 0;
        }

        return allSize != 0 && availableSize > limitSize;
    }

    public static String getExceptionCauseString(final Throwable ex) {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final PrintStream ps = new PrintStream(bos);

        try {
            // print directly
            Throwable t = ex;
            while (t.getCause() != null) {
                t = t.getCause();
            }
            t.printStackTrace(ps);
            return toVisualString(bos.toString());
        } finally {
            try {
                bos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static String toVisualString(String src) {
        boolean cutFlg = false;

        if (null == src) {
            return null;
        }

        char[] chr = src.toCharArray();
        if (null == chr) {
            return null;
        }

        int i = 0;
        for (; i < chr.length; i++) {
            if (chr[i] > 127) {
                chr[i] = 0;
                cutFlg = true;
                break;
            }
        }

        if (cutFlg) {
            return new String(chr, 0, i);
        } else {
            return src;
        }
    }

    public static boolean checkPermission(Context context, String permission) {
        boolean result = false;
        if (Build.VERSION.SDK_INT >= 23) {
            try {
                Class<?> clazz = Class.forName("android.content.Context");
                Method method = clazz.getMethod("checkSelfPermission", String.class);
                int rest = (Integer) method.invoke(context, permission);
                if (rest == PackageManager.PERMISSION_GRANTED) {
                    result = true;
                } else {
                    result = false;
                }
            } catch (Exception e) {
                result = false;
            }
        } else {
            PackageManager pm = context.getPackageManager();
            if (pm.checkPermission(permission, context.getPackageName()) == PackageManager.PERMISSION_GRANTED) {
                result = true;
            }
        }
        return result;
    }

    public static String getUDID(Context context) {
        try {
            org.json.JSONObject json = new org.json.JSONObject();
            android.telephony.TelephonyManager tm = (android.telephony.TelephonyManager) context
                    .getSystemService(Context.TELEPHONY_SERVICE);
            String device_id = null;
            if (checkPermission(context, Manifest.permission.READ_PHONE_STATE)) {
                device_id = tm.getDeviceId();
            }
            String wifiMac = getWifiMac(context);
            if (!TextUtils.isEmpty(wifiMac) && !"aa-aa-aa-aa-aa-aa".equals(wifiMac)) {
                json.put("mac", wifiMac);
            }
            if (TextUtils.isEmpty(device_id)) {
                device_id = wifiMac;
            }
            if (TextUtils.isEmpty(device_id)) {
                device_id = android.provider.Settings.Secure.getString(context.getContentResolver(),
                        android.provider.Settings.Secure.ANDROID_ID);
            }
            json.put("device_id", device_id);
            return json.toString();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    private static String getWifiMac(Context context) {
        String macSerial = null;
        String str = "";

        try {
            WifiManager wifiMgr = (WifiManager) context.getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
            WifiInfo info = (null == wifiMgr ? null : wifiMgr
                    .getConnectionInfo());
            if (null != info) {
                if (!TextUtils.isEmpty(info.getMacAddress())) {
                    macSerial = info.getMacAddress().replace(":", "-");
                }
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
        }

        if (macSerial == null || "02:00:00:00:00:00".equals(macSerial)) {
            try {
                Process pp = Runtime.getRuntime().exec(
                        "cat /sys/class/net/wlan0/address ");
                InputStreamReader ir = new InputStreamReader(pp.getInputStream());
                LineNumberReader input = new LineNumberReader(ir);

                for (; null != str; ) {
                    str = input.readLine();
                    if (str != null) {
                        macSerial = str.trim();
                        break;
                    }
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        if (macSerial == null || "02:00:00:00:00:00".equals(macSerial)) {
            macSerial = "aa-aa-aa-aa-aa-aa";
        }
        return macSerial;
    }

    /**
     * Return pseudo unique ID
     *
     * @return ID
     */
    public static String getUniquePsuedoID() {
        //we make this look like a valid IMEI
        String m_szDevIDShort = "35" +
                Build.BOARD.length() % 10 + Build.BRAND.length() % 10 +
                Build.CPU_ABI.length() % 10 + Build.DEVICE.length() % 10 +
                Build.DISPLAY.length() % 10 + Build.HOST.length() % 10 +
                Build.ID.length() % 10 + Build.MANUFACTURER.length() % 10 +
                Build.MODEL.length() % 10 + Build.PRODUCT.length() % 10 +
                Build.TAGS.length() % 10 + Build.TYPE.length() % 10 +
                Build.USER.length() % 10; //13 digits
        Log.d("json", "m_szDevIDShort:" + m_szDevIDShort);
        String serial = null;
        try {
            serial = android.os.Build.class.getField("SERIAL").get(null).toString();
            Log.d("json", "serial:" + serial);
            // Go ahead and return the serial for api => 9
            return new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString();
        } catch (Exception e) {
            // String needs to be initialized
            serial = "serial";
        }

        // Finally, combine the values we have found by using the UUID class to create a unique identifier
        return new UUID(m_szDevIDShort.hashCode(), serial.hashCode()).toString();
    }

    public static String getHardDevice() {
        StringBuffer sb = new StringBuffer();
        sb.append("主板" + Build.BOARD + "\n");
        sb.append(
                "系统启动程序版本号：" + Build.BOOTLOADER + "\n");
        sb.append(
                "系统定制商：" + Build.BRAND + "\n");
        sb.append(
                "cpu指令集：" + Build.CPU_ABI + "\n");
        sb.append(
                "cpu指令集2 " + Build.CPU_ABI2 + "\n");
        sb.append(
                "设置参数：" + Build.DEVICE + "\n");
        sb.append(
                "显示屏参数：" + Build.DISPLAY + "\n");
        sb.append(
                "无线电固件版本：" + Build.getRadioVersion() + "\n");
        sb.append(
                "硬件识别码:" + Build.FINGERPRINT + "\n");
        sb.append(
                "硬件名称：" + Build.HARDWARE + "\n");
        sb.append(
                "HOST:" + Build.HOST + "\n");
        sb.append(
                "修订版本列表：" + Build.ID + "\n");
        sb.append(
                "硬件制造商：" + Build.MANUFACTURER + "\n");
        sb.append(
                "版本：" + Build.MODEL + "\n");
        sb.append(
                "硬件序列号：" + Build.SERIAL + "\n");
        sb.append(
                "手机制造商：" + Build.PRODUCT + "\n");
        sb.append(
                "描述Build的标签：" + Build.TAGS + "\n");
        sb.append(
                "TIME:" + Build.TIME + "\n");
        sb.append(
                "builder类型：" + Build.TYPE + "\n");
        sb.append(
                "USER：" + Build.USER);
        return sb.toString();
    }


    public static boolean isDeviceRooted() {
        if (checkRootMethod1()) {
            return true;
        }
        if (checkRootMethod2()) {
            return true;
        }
        if (checkRootMethod3()) {
            return true;
        }
        return false;
    }

    public static boolean checkRootMethod1() {
        String buildTags = android.os.Build.TAGS;

        if (buildTags != null && buildTags.contains("test-keys")) {
            return true;
        }
        return false;
    }

    public static boolean checkRootMethod2() {
        try {
            File file = new File("/system/app/Superuser.apk");
            if (file.exists()) {
                return true;
            }
        } catch (Exception e) {
        }

        return false;
    }

    public static boolean checkRootMethod3() {
        if (new ExecShell().executeCommand(ExecShell.SHELL_CMD.check_su_binary) != null) {
            return true;
        } else {
            return false;
        }
    }


}



