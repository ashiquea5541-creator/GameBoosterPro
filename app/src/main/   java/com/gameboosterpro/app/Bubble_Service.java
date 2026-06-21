package com.gaming.booster;

import android.app.Service;
import android.content.Intent;
import android.graphics.PixelFormat;
import android.os.IBinder;
import android.view.*;
import android.widget.ImageView;

public class BubbleService extends Service {
    WindowManager wm;
    ImageView bubble;

    public IBinder onBind(Intent i) { return null; }

    public void onCreate() {
        super.onCreate();
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        bubble = new ImageView(this);
        bubble.setImageResource(android.R.drawable.btn_star_big_on);

        WindowManager.LayoutParams p = new WindowManager.LayoutParams(
                150, 150,
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);
        p.gravity = Gravity.TOP | Gravity.LEFT;
        p.x = 100; p.y = 100;

        bubble.setOnTouchListener(new View.OnTouchListener() {
            int x, y; float tx, ty;
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        x = p.x; y = p.y;
                        tx = e.getRawX(); ty = e.getRawY();
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        p.x = x + (int)(e.getRawX() - tx);
                        p.y = y + (int)(e.getRawY() - ty);
                        wm.updateViewLayout(bubble, p);
                        return true;
                }
                return false;
            }
        });
        wm.addView(bubble, p);
    }

    public void onDestroy() {
        super.onDestroy();
        if (bubble != null) wm.removeView(bubble);
    }
}
