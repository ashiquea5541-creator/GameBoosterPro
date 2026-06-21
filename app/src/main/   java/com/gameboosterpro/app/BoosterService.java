package com.gamebooster.pro;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.List;

public class BoosterService extends Service {
    
    Handler handler = new Handler();
    Runnable boosterRunnable;
    boolean isRooted = false;
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        isRooted = RootUtil.isRooted();
        startBooster();
        return START_STICKY;
    }
    
    void startBooster() {
        boosterRunnable = new Runnable() {
            @Override
            public void run() {
                // 1. RAM CLEAR - ROOT + NON ROOT DONO
                clearRam();
                
                // 2. CACHE DELETE - ROOT + NON ROOT DONO  
                clearCache();
                
                // 3. PING CHECK - DONO
                int ping = checkPing();
                
                // 4. ROOT ONLY FEATURES
                if (isRooted) {
                    setFPS90();           // Force 90Hz/120Hz if display supports
                    setDNSRoot();         // Change DNS to 1.1.1.1
                    setCPUMaxFreq();      // Lock CPU to max frequency
                    dropCachesRoot();     // Deep cache clear
                } else {
                    boostCPUNonRoot();    // Sirf priority badhao
                }
                
                handler.postDelayed(this, 5000);
            }
        };
        handler.post(boosterRunnable);
    }
    
    void clearRam() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
        if (processes != null) {
            for (ActivityManager.RunningAppProcessInfo processInfo : processes) {
                if (processInfo.importance > ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE) {
                    am.killBackgroundProcesses(processInfo.processName);
                }
            }
        }
        if (isRooted) runRootCommand("sync; echo 3 > /proc/sys/vm/drop_caches");
    }
    
    void clearCache() {
        try {
            File cacheDir = getCacheDir();
            deleteDir(cacheDir);
        } catch (Exception e) {}
        if (isRooted) runRootCommand("rm -rf /data/dalvik-cache/*");
    }
    
    // ROOT ONLY: Force 90 FPS if display supports
    void setFPS90() {
        runRootCommand("settings put system peak_refresh_rate 90.0");
        runRootCommand("settings put system min_refresh_rate 90.0");
        runRootCommand("settings put system user_refresh_rate 90.0");
    }
    
    // ROOT ONLY: Change DNS
    void setDNSRoot() {
        runRootCommand("settings put global private_dns_mode hostname");
        runRootCommand("settings put global private_dns_specifier one.one.one.one");
    }
    
    // ROOT ONLY: CPU Max Performance
    void setCPUMaxFreq() {
        runRootCommand("echo performance > /sys/devices/system/cpu/cpu0/cpufreq/scaling_governor");
        runRootCommand("echo performance > /sys/devices/system/cpu/cpu4/cpufreq/scaling_governor");
    }
    
    // ROOT ONLY: Deep cache drop
    void dropCachesRoot() {
        runRootCommand("sync; echo 3 > /proc/sys/vm/drop_caches");
    }
    
    // NON-ROOT: Sirf thread priority
    void boostCPUNonRoot() {
        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_DISPLAY);
    }
    
    int checkPing() {
        try {
            long start = System.currentTimeMillis();
            InetAddress.getByName("1.1.1.1").isReachable(1000);
            long end = System.currentTimeMillis();
            return (int) (end - start);
        } catch (IOException e) {
            return 999;
        }
    }
    
    boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            for (String child : children) {
                boolean success = deleteDir(new File(dir, child));
                if (!success) return false;
            }
        }
        return dir.delete();
    }
    
    void runRootCommand(String cmd) {
        try {
            Process p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            os.writeBytes(cmd + "\n");
            os.writeBytes("exit\n");
            os.flush();
            p.waitFor();
        } catch (Exception e) {}
    }
    
    @Override
    public void onDestroy() {
        handler.removeCallbacks(boosterRunnable);
        super.onDestroy();
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
