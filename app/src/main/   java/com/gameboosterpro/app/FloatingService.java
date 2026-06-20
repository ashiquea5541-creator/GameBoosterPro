package com.gameboosterpro.app;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;
import java.io.DataOutputStream;

public class FloatingService extends Service {
    private WindowManager windowManager;
    private View floatingView;
    private View expandedView;
    private WindowManager.LayoutParams params;
    private boolean isExpanded = false;
    private boolean aimbotOn = false;
    private boolean headshotOn = false;
    private boolean noRecoilOn = false;
    private boolean wallhackOn = false;
    private boolean unlocked = false;

    @Override
    public IBinder onBind(Intent intent) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_bubble, null);
        expandedView = LayoutInflater.from(this).inflate(R.layout.floating_expanded, null);
        
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O ?
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        
        params.gravity = Gravity.TOP | Gravity.LEFT;
        params.x = 0;
        params.y = 100;
        
        windowManager.addView(floatingView, params);
        setupTouch();
        setupButtons();
    }
    
    private void setupTouch() {
        floatingView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        return true;
                    case MotionEvent.ACTION_UP:
                        if (Math.abs(event.getRawX() - initialTouchX) < 10 && Math.abs(event.getRawY() - initialTouchY) < 10) {
                            toggleMenu();
                        }
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(floatingView, params);
                        return true;
                }
                return false;
            }
        });
    }
    
    private void toggleMenu() {
        if (!isExpanded) {
            windowManager.removeView(floatingView);
            windowManager.addView(expandedView, params);
            isExpanded = true;
        } else {
            windowManager.removeView(expandedView);
            windowManager.addView(floatingView, params);
            isExpanded = false;
        }
    }
    
    private void setupButtons() {
        expandedView.findViewById(R.id.btnClose).setOnClickListener(v -> toggleMenu());
        
        TextView tvAimbot = expandedView.findViewById(R.id.tvAimbot);
        tvAimbot.setOnClickListener(v -> {
            aimbotOn = !aimbotOn;
            tvAimbot.setText("🎯 Aimbot: " + (aimbotOn ? "ON" : "OFF"));
            runRootCommand(aimbotOn ? "setprop wrap.com.dts.freefireth aimbot_on" : "setprop wrap.com.dts.freefireth aimbot_off");
        });
        
        TextView tvHeadshot = expandedView.findViewById(R.id.tvHeadshot);
        tvHeadshot.setOnClickListener(v -> {
            headshotOn = !headshotOn;
            tvHeadshot.setText("🔫 Headshot: " + (headshotOn ? "ON" : "OFF"));
            runRootCommand(headshotOn ? "settings put global headshot_mode 1" : "settings put global headshot_mode 0");
        });
        
        TextView tvNoRecoil = expandedView.findViewById(R.id.tvNoRecoil);
        tvNoRecoil.setOnClickListener(v -> {
            noRecoilOn = !noRecoilOn;
            tvNoRecoil.setText("💥 No Recoil: " + (noRecoilOn ? "ON" : "OFF"));
            runRootCommand(noRecoilOn ? "echo 0 > /proc/recoil" : "echo 1 > /proc/recoil");
        });
        
        TextView tvWallhack = expandedView.findViewById(R.id.tvWallhack);
        tvWallhack.setOnClickListener(v -> {
            if (!MainActivity.hasRoot && !unlocked) {
                Toast.makeText(this, "❌ Root Required for Wallhack", Toast.LENGTH_SHORT).show();
                return;
            }
            wallhackOn = !wallhackOn;
            tvWallhack.setText("👁️ Wallhack: " + (wallhackOn ? "ON" : "OFF"));
            runRootCommand(wallhackOn ? "chmod 777 /data/data/com.dts.freefireth/" : "chmod 755 /data/data/com.dts.freefireth/");
        });
        
        expandedView.findViewById(R.id.btnUnlock).setOnClickListener(v -> {
            unlocked = true;
            Toast.makeText(this, "✅ All Features Unlocked!", Toast.LENGTH_SHORT).show();
        });
    }
    
    private void runRootCommand(String cmd) {
        if (!MainActivity.hasRoot) return;
        try {
            Process p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            os.writeBytes(cmd + "\n");
            os.writeBytes("exit\n");
            os.flush();
        } catch (Exception e) {}
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (floatingView != null) windowManager.removeView(floatingView);
        if (expandedView != null && isExpanded) windowManager.removeView(expandedView);
    }
}
