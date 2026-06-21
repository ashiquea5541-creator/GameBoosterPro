package com.gameboosterpro.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import androidx.core.app.NotificationCompat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FloatingService extends Service {

    private WindowManager windowManager;
    private View bubbleView;
    private TextView tvBubblePing;
    private WindowManager.LayoutParams params;
    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean isPingRunning = true;
    
    private BroadcastReceiver statusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.hasExtra("status")) {
                // MainActivity se update aya
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(1, createNotification());
        
        bubbleView = LayoutInflater.from(this).inflate(R.layout.bubble_layout, null);
        tvBubblePing = bubbleView.findViewById(R.id.tvBubblePing);
        
        int layoutFlag;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            layoutFlag = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
        } else {
            layoutFlag = WindowManager.LayoutParams.TYPE_PHONE;
        }
        
        params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        
        params.gravity = Gravity.TOP | Gravity.START;
        params.x = 100;
        params.y = 100;
        
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        windowManager.addView(bubbleView, params);
        
        registerReceiver(statusReceiver, new IntentFilter("GAMEBOOSTER_UPDATE"), Context.RECEIVER_NOT_EXPORTED);
        
        setupBubbleTouch();
        startPingMonitor();
    }
    
    private void setupBubbleTouch() {
        bubbleView.setOnTouchListener(new View.OnTouchListener() {
            private int initialX, initialY;
            private float initialTouchX, initialTouchY;
            private long touchStartTime;
            
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        initialX = params.x;
                        initialY = params.y;
                        initialTouchX = event.getRawX();
                        initialTouchY = event.getRawY();
                        touchStartTime = System.currentTimeMillis();
                        return true;
                        
                    case MotionEvent.ACTION_MOVE:
                        params.x = initialX + (int) (event.getRawX() - initialTouchX);
                        params.y = initialY + (int) (event.getRawY() - initialTouchY);
                        windowManager.updateViewLayout(bubbleView, params);
                        return true;
                        
                    case MotionEvent.ACTION_UP:
                        long touchTime = System.currentTimeMillis() - touchStartTime;
                        if (touchTime < 200) {
                            Intent intent = new Intent(FloatingService.this, MainActivity.class);
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                            startActivity(intent);
                        }
                        return true;
                }
                return false;
            }
        });
    }
    
    private void startPingMonitor() {
        executor.execute(() -> {
            while (isPingRunning) {
                try {
                    long start = System.currentTimeMillis();
                    Process p = Runtime.getRuntime().exec("ping -c 1 -W 1 8.8.8.8");
                    int exit = p.waitFor();
                    long ping = System.currentTimeMillis() - start;
                    
                    handler.post(() -> {
                        if (exit == 0) {
                            tvBubblePing.setText(ping + "ms");
                            if (ping < 50) tvBubblePing.setBackgroundResource(R.drawable.bubble_green);
                            else if (ping < 100) tvBubblePing.setBackgroundResource(R.drawable.bubble_yellow);
                            else tvBubblePing.setBackgroundResource(R.drawable.bubble_red);
                        } else {
                            tvBubblePing.setText("--");
                            tvBubblePing.setBackgroundResource(R.drawable.bubble_red);
                        }
                    });
                    Thread.sleep(3000);
                } catch (Exception e) {
                    handler.post(() -> tvBubblePing.setText("Err"));
                }
            }
        });
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    "bubble_channel",
                    "Game Booster Bubble",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
    
    private Notification createNotification() {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        
        return new NotificationCompat.Builder(this, "bubble_channel")
                .setContentTitle("Game Booster Active")
                .setContentText("Ping monitor running")
                .setSmallIcon(android.R.drawable.ic_menu_info_details)
                .setContentIntent(pi)
                .build();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isPingRunning = false;
        executor.shutdown();
        if (bubbleView != null) windowManager.removeView(bubbleView);
        try { unregisterReceiver(statusReceiver); } catch (Exception e) {}
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
