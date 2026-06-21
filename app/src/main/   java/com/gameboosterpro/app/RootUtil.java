package com.gamebooster.pro;
import java.io.File;
public class RootUtil {
    public static boolean isRooted() {
        String[] paths = {"/system/app/Superuser.apk", "/sbin/su", "/system/bin/su", 
                          "/system/xbin/su", "/data/local/xbin/su", "/data/local/bin/su", 
                          "/system/sd/xbin/su", "/system/bin/failsafe/su", "/data/local/su"};
        for (String path : paths) {
            if (new File(path).exists()) return true;
        }
        return false;
    }
}
