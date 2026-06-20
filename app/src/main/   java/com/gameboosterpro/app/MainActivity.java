package com.gameboosterpro.app;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.io.DataOutputStream;

public class MainActivity extends Activity {
    
    public static boolean hasRoot = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hasRoot = isRooted();
        setContentView(R.layout.activity_main);
        
        TextView tvRoot = findViewById(R.id.tvRootStatus);
        tvRoot.setText(hasRoot? "✅ Root Access: GRANTED" : "❌ Root Access: NOT FOUND");
        
        Button btnStart = findViewById(R.id.btnStart);
        btnStart.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M &&!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 1);
            } else {
                startService(new Intent(MainActivity.this, FloatingService.class));
                finish();
            }
        });
    }
    
    public static boolean isRooted() {
        try {
            Process p = Runtime.getRuntime().exec("su");
            DataOutputStream os = new DataOutputStream(p.getOutputStream());
            os.writeBytes("exit\n");
            os.flush();
            return p.waitFor() == 0;
        } catch (Exception e) {
            return false;
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 1) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                startService(new Intent(MainActivity.this, FloatingService.class));
                finish();
            }
        }
    }
}
