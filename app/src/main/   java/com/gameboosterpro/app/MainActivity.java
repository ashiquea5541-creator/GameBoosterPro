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
    
    TextView statusText, ramText, pingText, rootText;
    Button gameModeBtn, bubbleBtn;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        statusText = findViewById(R.id.statusText);
        ramText = findViewById(R.id.ramText);
        pingText = findViewById(R.id.pingText);
        rootText = findViewById(R.id.rootText);
        gameModeBtn = findViewById(R.id.gameModeBtn);
        bubbleBtn = findViewById(R.id.bubbleBtn);
        
        boolean root = RootUtil.isRooted();
        rootText.setText("ROOT: " + (root ? "YES - FULL POWER" : "NO - BASIC MODE"));
        
        startService(new Intent(this, BoosterService.class));
        
        gameModeBtn.setOnClickListener(v -> startGameMode());
        bubbleBtn.setOnClickListener(v -> showBubble());
    }
    
    void startGameMode() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (!Settings.System.canWrite(this)) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                    Toast.makeText(this, "Allow permission, then click again", Toast.LENGTH_LONG).show();
                    return;
                }
            }
            Settings.System.putInt(getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 255);
            
            if (RootUtil.isRooted()) {
                statusText.setText("Status: RED MAGIC MODE + ROOT BOOST");
            } else {
                statusText.setText("Status: GAME MODE ACTIVE");
            }
        } catch (Exception e) {
            statusText.setText("Status: ERROR");
        }
    }
    
    void showBubble() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(intent);
                Toast.makeText(this, "Allow overlay, then click again", Toast.LENGTH_LONG).show();
                return;
            }
        }
        startService(new Intent(this, BubbleService.class));
    }
}
