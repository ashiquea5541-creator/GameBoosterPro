package com.gamebooster.pro;

import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import java.io.DataOutputStream;
import java.io.File;

public class BoosterService extends Service {
    
    private Handler handler;
    private Runnable boosterRunnable;
    private boolean isRooted = false;
    
    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler();
        isRooted = RootUtil.isRooted();
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startBooster();
        return START_STICKY;
    }
    
    private void startBooster() {
        boosterRunnable = new Runnable() {
            @Override
            public void run() {
                try {
                    clearRam();
                    clearCache();
                    if (isRooted) {
                        runRootCommand("sync; echo 3 > /proc/sys/vm/drop_caches");
                        runRootCommand("settings put system peak_refresh_rate 90.0");
                        runRootCommand("settings put global private_dns_mode hostname");
                        runRootCommand("settings put global private_dns_specifier one.one.one.one");
                    }
                } catch (Exception e) {
                    // Kuch bhi ho, crash nahi karna
                }
                handler.postDelayed(this, 5000);
            }
        };
        handler.post(boosterRunnable);
    }
    
    private void clearRam() {
        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            if (am != null && am.getRunningAppProcesses() != null) {
                for (ActivityManager.RunningAppProcessInfo processInfo : am.getRunningAppProcesses()) {
                    if (processInfo.importance > ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE) {
                        am.killBackgroundProcesses(processInfo.processName);
                    }
                }
            }
        } catch (Exception e) {}
    }
    
    private void clearCache() {
        try {
            deleteDir(getCacheDir());
            deleteDir(getExternalCacheDir());
        } catch (Exception e) {}
    }
    
    private boolean deleteDir(File dir) {
        if (dir == null || !dir.exists()) return true;
        if (dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    deleteDir(new File(dir, child));
                }
            }
        }
        return dir.delete();
    }
    
    private void runRootCommand(String cmd) {
        if (!isRooted) return;
        try {
            Process p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            os.writeBytes(cmd + "\n");
            os.writeBytes("exit\n");
            os.flush();
            os.close();
            p.waitFor();
        } catch (Exception e) {}
    }
    
    @Override
    public void onDestroy() {
        if (handler != null && boosterRunnable != null) {
            handler.removeCallbacks(boosterRunnable);
        }
        super.onDestroy();
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
