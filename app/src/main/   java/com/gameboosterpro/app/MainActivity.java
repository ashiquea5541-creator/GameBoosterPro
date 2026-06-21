package com.gameboosterpro.app;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import java.io.DataOutputStream;

public class MainActivity extends AppCompatActivity {
    
    public static boolean hasRoot = false;
    TextView tvStatus;
    Button btnStartBubble, btnStopBubble, btnKillProcesses;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        hasRoot = isRooted();
        
        // XML se sahi IDs le rahe hain
        tvStatus = findViewById(R.id.tvStatus);
        btnStartBubble = findViewById(R.id.btnStartBubble);
        btnStopBubble = findViewById(R.id.btnStopBubble);
        btnKillProcesses = findViewById(R.id.btnKillProcesses);
        
        // Root status dikhao
        tvStatus.setText(hasRoot ? "✅ Root: GRANTED | Bubble: INACTIVE" : "❌ Root: NOT FOUND | Bubble: INACTIVE");
        
        btnStartBubble.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivityForResult(intent, 1);
            } else {
                startService(new Intent(MainActivity.this, FloatingService.class));
                tvStatus.setText("Bubble Overlay: ACTIVE");
                btnStartBubble.setEnabled(false);
                btnStopBubble.setEnabled(true);
                finish();
            }
        });
        
        btnStopBubble.setOnClickListener(v -> {
            stopService(new Intent(MainActivity.this, FloatingService.class));
            tvStatus.setText("Bubble Overlay: INACTIVE");
            btnStartBubble.setEnabled(true);
            btnStopBubble.setEnabled(false);
        });
        
        btnKillProcesses.setOnClickListener(v -> {
            Toast.makeText(this, "Killing background processes...", Toast.LENGTH_SHORT).show();
            // Yahan kill processes ka code daalna
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
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && Settings.canDrawOverlays(this)) {
                startService(new Intent(MainActivity.this, FloatingService.class));
                tvStatus.setText("Bubble Overlay: ACTIVE");
                btnStartBubble.setEnabled(false);
                btnStopBubble.setEnabled(true);
                finish();
            }
        }
    }
}
