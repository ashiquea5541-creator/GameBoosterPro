package com.gameboosterpro.app

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var btnStartBubble: Button
    private lateinit var btnStopBubble: Button
    private lateinit var btnKillProcesses: Button
    private lateinit var tvStatus: TextView

    private val overlayPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (Settings.canDrawOverlays(this)) {
            startBubbleService()
        } else {
            Toast.makeText(this, "Overlay permission denied", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnStartBubble = findViewById(R.id.btnStartBubble)
        btnStopBubble = findViewById(R.id.btnStopBubble)
        btnKillProcesses = findViewById(R.id.btnKillProcesses)
        tvStatus = findViewById(R.id.tvStatus)

        btnStartBubble.setOnClickListener {
            handleStartBubble()
        }

        btnStopBubble.setOnClickListener {
            stopBubbleService()
        }

        btnKillProcesses.setOnClickListener {
            killBackgroundProcesses()
        }

        updateStatus()
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun handleStartBubble() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            overlayPermissionLauncher.launch(intent)
        } else {
            startBubbleService()
        }
    }

    private fun startBubbleService() {
        val intent = Intent(this, BubbleService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
        Toast.makeText(this, "Bubble overlay started", Toast.LENGTH_SHORT).show()
        updateStatus()
    }

    private fun stopBubbleService() {
        val intent = Intent(this, BubbleService::class.java)
        stopService(intent)
        Toast.makeText(this, "Bubble overlay stopped", Toast.LENGTH_SHORT).show()
        updateStatus()
    }

    private fun killBackgroundProcesses() {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val runningApps = activityManager.runningAppProcesses ?: return
        var killedCount = 0
        for (processInfo in runningApps) {
            if (processInfo.importance >= ActivityManager.RunningAppProcessInfo.IMPORTANCE_SERVICE
                && processInfo.processName != packageName
            ) {
                activityManager.killBackgroundProcesses(processInfo.processName)
                killedCount++
            }
        }
        Toast.makeText(this, "Killed $killedCount background process(es)", Toast.LENGTH_SHORT).show()
    }

    private fun updateStatus() {
        val running = BubbleService.isRunning
        tvStatus.text = if (running) "Bubble Overlay: ACTIVE" else "Bubble Overlay: INACTIVE"
        btnStartBubble.isEnabled = !running
        btnStopBubble.isEnabled = running
    }
}
