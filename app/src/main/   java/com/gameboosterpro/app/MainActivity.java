package com.gamebooster.pro;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        TextView rootText = findViewById(R.id.rootText);
        Button gameModeBtn = findViewById(R.id.gameModeBtn);
        Button bubbleBtn = findViewById(R.id.bubbleBtn);
        
        boolean root = RootUtil.isRooted();
        rootText.setText(root ? "ROOT: YES - FULL MODE" : "ROOT: NO - BASIC MODE");
        
        // Service start karo, lekin UI se connect mat karo
        startService(new Intent(this, BoosterService.class));
        
        gameModeBtn.setOnClickListener(v -> {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    if (!Settings.System.canWrite(this)) {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                        Toast.makeText(this, "Permission do, phir dubara dabao", Toast.LENGTH_LONG).show();
                        return;
                    }
                }
                Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 255);
                
                // Bina root wale ke liye DNS setting khol do
                if (!root && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    startActivity(new Intent(Settings.ACTION_WIRELESS_SETTINGS));
                    Toast.makeText(this, "Private DNS → 1.1.1.1 set karo", Toast.LENGTH_LONG).show();
                }
                Toast.makeText(this, "Game Mode Started", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
        
        bubbleBtn.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.canDrawOverlays(this)) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                            Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                    return;
                }
            }
            startService(new Intent(this, BubbleService.class));
        });
    }
}
