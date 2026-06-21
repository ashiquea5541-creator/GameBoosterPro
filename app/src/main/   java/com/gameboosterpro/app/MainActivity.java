package com.gameboosterpro.app;

import androidx.appcompat.app.AppCompatActivity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    
    public static boolean hasRoot = false;
    private boolean pingRunning = false;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler mainHandler = new Handler(Looper.getMainLooper());
    
    TextView tvStatus, tvPing;
    Button btnStartBubble, btnStopBubble, btnKillProcesses, btnSetDNS, btnResetDNS, btnPingTest;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initViews();
        checkRootStatus();
        setupClickListeners();
        startPingMonitor();
    }
    
    private void initViews() {
        tvStatus = findViewById(R.id.tvStatus);
        tvPing = findViewById(R.id.tvPing);
        btnStartBubble = findViewById(R.id.btnStartBubble);
        btnStopBubble = findViewById(R.id.btnStopBubble);
        btnKillProcesses = findViewById(R.id.btnKillProcesses);
        btnSetDNS = findViewById(R.id.btnSetDNS);
        btnResetDNS = findViewById(R.id.btnResetDNS);
        btnPingTest = findViewById(R.id.btnPingTest);
    }
    
    private void checkRootStatus() {
        hasRoot = isRooted();
        updateStatusText("Root: " + (hasRoot ? "GRANTED" : "NONE"));
    }
    
    private void updateStatusText(String msg) {
        boolean serviceRunning = isServiceRunning(FloatingService.class);
        String bubbleText = serviceRunning ? "Bubble: ACTIVE" : "Bubble: INACTIVE";
        tvStatus.setText(msg + " | " + bubbleText);
        btnStartBubble.setEnabled(!serviceRunning);
        btnStopBubble.setEnabled(serviceRunning);
    }
    
    private void setupClickListeners() {
        btnStartBubble.setOnClickListener(v -> startBubbleService());
        btnStopBubble.setOnClickListener(v -> stopBubbleService());
        btnKillProcesses.setOnClickListener(v -> killBackgroundProcesses());
        btnSetDNS.setOnClickListener(v -> setGamingDNS());
        btnResetDNS.setOnClickListener(v -> resetDNS());
        btnPingTest.setOnClickListener(v -> togglePingMonitor());
    }
    
    // ========== PING MONITOR ==========
    private void startPingMonitor() {
        pingRunning = true;
        btnPingTest.setText("Stop Ping Monitor");
        monitorPing();
    }
    
    private void togglePingMonitor() {
        if(pingRunning) {
            pingRunning = false;
            btnPingTest.setText("Start Ping Monitor");
            tvPing.setText("Ping: Stopped");
            tvPing.setTextColor(0xFFE2E8F0);
        } else {
            startPingMonitor();
        }
    }
    
    private void monitorPing() {
        executor.execute(() -> {
            while(pingRunning) {
                try {
                    long start = System.currentTimeMillis();
                    Process p = Runtime.getRuntime().exec("ping -c 1 -W 1 8.8.8.8");
                    int exit = p.waitFor();
                    long ping = System.currentTimeMillis() - start;
                    
                    mainHandler.post(() -> {
                        if(exit == 0) {
                            tvPing.setText("Ping: " + ping + " ms");
                            if(ping < 50) tvPing.setTextColor(0xFF4ADE80);
                            else if(ping < 100) tvPing.setTextColor(0xFFFACC15);
                            else tvPing.setTextColor(0xFFEF4444);
                        } else {
                            tvPing.setText("Ping: Timeout");
                            tvPing.setTextColor(0xFFEF4444);
                        }
                    });
                    Thread.sleep(2000);
                } catch (Exception e) {
                    mainHandler.post(() -> tvPing.setText("Ping: Error"));
                }
            }
        });
    }
    
    // ========== DNS CHANGER ==========
    private void setGamingDNS() {
        if(!hasRoot) {
            updateStatusText("❌ Root required for DNS change");
            return;
        }
        
        btnSetDNS.setEnabled(false);
        executor.execute(() -> {
            try {
                Process su = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(su.getOutputStream());
                os.writeBytes("settings put global private_dns_mode hostname\n");
                os.writeBytes("settings put global private_dns_specifier 1dot1dot1dot1.cloudflare-dns.com\n");
                os.writeBytes("ndc resolver setnetdns 1 \"\" 1.1.1.1 1.0.0.1\n");
                os.writeBytes("exit\n");
                os.flush();
                int exit = su.waitFor();
                
                mainHandler.post(() -> {
                    if(exit == 0) updateStatusText("✅ DNS: Cloudflare 1.1.1.1 Active");
                    else updateStatusText("❌ DNS change failed");
                    btnSetDNS.setEnabled(true);
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    updateStatusText("❌ DNS Error: " + e.getMessage());
                    btnSetDNS.setEnabled(true);
                });
            }
        });
    }
    
    private void resetDNS() {
        if(!hasRoot) {
            updateStatusText("❌ Root required");
            return;
        }
        
        btnResetDNS.setEnabled(false);
        executor.execute(() -> {
            try {
                Process su = Runtime.getRuntime().exec("su");
                DataOutputStream os = new DataOutputStream(su.getOutputStream());
                os.writeBytes("settings put global private_dns_mode opportunistic\n");
                os.writeBytes("ndc resolver clearnetdns 1\n");
                os.writeBytes("exit\n");
                os.flush();
                su.waitFor();
                
                mainHandler.post(() -> {
                    updateStatusText("✅ DNS: Reset to Default");
                    btnResetDNS.setEnabled(true);
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    updateStatusText("❌ Reset failed");
                    btnResetDNS.setEnabled(true);
                });
            }
        });
    }
    
    // ========== KILL PROCESSES ==========
    private void killBackgroundProcesses() {
        btnKillProcesses.setEnabled(false);
        btnKillProcesses.setText("Optimizing...");
        
        executor.execute(() -> {
            String resultMsg;
            long startMem = getAvailableMemory();
            
            if (hasRoot) {
                resultMsg = killWithRoot();
            } else {
                resultMsg = killWithoutRoot();
            }
            
            long freedMB = (getAvailableMemory() - startMem) / 1024 / 1024;
            String finalMsg = resultMsg + " | +" + freedMB + "MB";
            
            mainHandler.post(() -> {
                updateStatusText(finalMsg);
                btnKillProcesses.setEnabled(true);
                btnKillProcesses.setText("Kill Background Apps");
            });
        });
    }
    
    private String killWithRoot() {
        try {
            Process su = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(su.getOutputStream());
            os.writeBytes("am kill-all\n");
            os.writeBytes("sync && echo 3 > /proc/sys/vm/drop_caches\n");
            os.writeBytes("exit\n");
            os.flush();
            return su.waitFor() == 0 ? "✅ Root: RAM Cleared" : "❌ Failed";
        } catch (Exception e) {
            return "❌ Error";
        }
    }
    
    private String killWithoutRoot() {
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> processes = am.getRunningAppProcesses();
        String myPkg = getPackageName();
        int count = 0;
        
        if (processes != null) {
            for (ActivityManager.RunningAppProcessInfo info : processes) {
                if (!info.processName.equals(myPkg) && 
                    info.importance >= ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE) {
                    am.killBackgroundProcesses(info.processName);
                    count++;
                }
            }
        }
        return "✅ Killed " + count + " apps";
    }
    
    // ========== BUBBLE SERVICE ==========
    private void startBubbleService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            startActivityForResult(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package
