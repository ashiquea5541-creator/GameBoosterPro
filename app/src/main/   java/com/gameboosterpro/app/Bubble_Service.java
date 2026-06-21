package com.gaming.booster;

import android.app.*;
import android.content.Context;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.view.*;
import android.widget.*;

public class BubbleService extends Service {
    private WindowManager wm;
    private LinearLayout bubble;

    @Override
    public IBinder onBind(Intent i) { return null; }

    @Override
    public void onCreate() {
        super.onCreate();
        
        String CHANNEL_ID = "booster";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(CHANNEL_ID, "Booster", NotificationManager.IMPORTANCE_LOW);
            ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(ch);
        }
        startForeground(1, new Notification.Builder(this, CHANNEL_ID)
            .setContentTitle("Game Booster On")
            .setSmallIcon(android.R.drawable.ic_dialog_info).build());

        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        bubble = new LinearLayout(this);
        bubble.setBackgroundColor(0xDD000000);
        bubble.setPadding(30, 30, 30, 30);
        
        TextView tv = new TextView(this);
        tv.setText("BOOST");
        tv.setTextColor(0xFFFFFFFF);
        tv.setOnClickListener(v -> {
            Runtime.getRuntime().gc();
            Toast.makeText(this, "Boosted", 0).show();
        });
        bubble.addView(tv);

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? 
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : 
            WindowManager.LayoutParams.TYPE_PHONE;

        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
            -2, -2, type, WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE, PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.LEFT;
        p.x = 100; p.y = 300;

        bubble.setOnTouchListener(new View.OnTouchListener() {
            float x, y; int px, py;
            public boolean onTouch(View v, MotionEvent e) {
                if (e.getAction() == MotionEvent.ACTION_DOWN) {
                    x = e.getRawX(); y = e.getRawY(); px = p.x; py = p.y; return true;
                }
                if (e.getAction() == MotionEvent.ACTION_MOVE) {
                    p.x = px + (int)(e.getRawX() - x);
                    p.y = py + (int)(e.getRawY() - y);
                    wm.updateViewLayout(bubble, p); return true;
                }
                return false;
            }
        });
        wm.addView(bubble, p);
    }
    
    @Override
    public void onDestroy() { 
        if(bubble != null) wm.removeView(bubble); 
        super.onDestroy(); 
    }
}
