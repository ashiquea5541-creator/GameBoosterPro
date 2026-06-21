package com.gaming.booster;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Button btn = new Button(this);
        btn.setText("Start Bubble");
        btn.setOnClickListener(v -> {
            if (!Settings.canDrawOverlays(this)) {
                Intent i = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getPackageName()));
                startActivity(i);
                Toast.makeText(this, "Permission do, phir dubara button dabao", 1).show();
            } else {
                startService(new Intent(this, BubbleService.class));
                finish();
            }
        });
        setContentView(btn);
    }
}
