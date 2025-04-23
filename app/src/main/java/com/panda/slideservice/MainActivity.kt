package com.panda.slideservice

import android.Manifest
import android.app.ActivityManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.Settings
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModelProvider
import com.panda.slideservice.databinding.ActivityMainBinding
import com.panda.slideservice.service.VocalService
import com.panda.slideservice.viewmodel.ThreadViewModel

class MainActivity : AppCompatActivity() {
    private var defaultPreferences: DefaultPreferences? = null
    private var viewModel: ThreadViewModel?=null
    private lateinit var binding: ActivityMainBinding
    private var cameraId: String? = null
    private var camManager: CameraManager? = null
    private var vocalService: VocalService? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)
        viewModel = ViewModelProvider(this)[ThreadViewModel::class.java]
        defaultPreferences = DefaultPreferences(this)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initCam()
        initVIews()
    }
    private var isBound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as VocalService.LocalBinder
            vocalService = binder.getService()
            isBound = true

            // Ví dụ gọi hàm từ Service
            vocalService?.startDetectionExternally()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            vocalService = null
            isBound = false
            vocalService?.stopDetectionExternally()
        }
    }
    private fun initCam() = try {
        camManager = getSystemService(CAMERA_SERVICE) as CameraManager
        cameraId = camManager?.cameraIdList?.get(0)
    } catch (ignored: java.lang.Exception) {
    }

    private fun initVIews() {
        binding.btnStartService.setOnClickListener {
            handleCheckOpenDialog()
        }
    }
    private fun handleCheckOpenDialog() {
        try {
            DetectClapClap.handler.removeMessages(0)
        } catch (e: java.lang.Exception) {
        }

        if (!checkRecord()) {
            requestPermissions(arrayOf("android.permission.RECORD_AUDIO"), 101)
        } else if (!checkOverLay()) {
            checkPermissionOverlay()
        } else {
            if (!checkBoolAndroid13()) {
                checkAndroid13()
            } else {
                handleButton()
            }
        }
    }
    private fun handleButton() {
        if (isMyServiceRunning(VocalService::class.java)) {
            defaultPreferences!!.save("StopService", "1")

            MediaManager.getInstance().stopSound()
            closeFlash()
            viewModel!!.setVibrator(false, this)
            DetectClapClap.isRunning = false
            DetectClapClap.isRunning = false
            DetectClapClap.isClapped = false
            DetectClapClap.clapping = 0
        } else {
            DetectClapClap.isRunning = true
            defaultPreferences!!.save("StopService", "0")

        }
        check()
    }
    private fun stopService() {
        DetectClapClap.mIsRecording = false
        stopService(Intent(this, VocalService::class.java))
        Log.d("STOP SERVICE", "STOP Service")
        defaultPreferences!!.save("StopService", "1")

        viewModel?.setVibrator(false, this)
        closeFlash()
    }
    private fun closeFlash() {
        DetectClapClap.handler.removeCallbacksAndMessages(null)
        try {
            if (cameraId != null) {
                if (camManager != null) {
                    camManager?.cameraIdList?.get(0)?.let { camManager?.setTorchMode(it, false) }
                }
            }
        } catch (e: java.lang.Exception) {
        }
    }

    fun checkPermissionOverlay() {
        if (!Settings.canDrawOverlays(this)) {

            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName())
            )
            startActivityForResult(intent, 5469)
        }
    }
    private fun checkAndroid13() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 102)
        }
    }

    private fun checkBoolAndroid13(): Boolean {
        if (Build.VERSION.SDK_INT >= 33) {
            return if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                false
            } else {
                true
            }
        }
        return true
    }

    fun checkOverLay(): Boolean {
        return Settings.canDrawOverlays(this)
    }
    fun checkRecord(): Boolean {
        return ActivityCompat.checkSelfPermission(this, "android.permission.RECORD_AUDIO") == 0
    }
    private fun isMyServiceRunning(cls: Class<*>): Boolean {
        for (runningServiceInfo in (getSystemService(ACTIVITY_SERVICE) as ActivityManager).getRunningServices(
            Int.MAX_VALUE
        )) {
            if (cls.name == runningServiceInfo.service.className) {
                return true
            }
        }
        return false
    }

    fun check() {
        if(DetectClapClap.isRunning){
            vocalService?.stopDetectionExternally()
        }else{
            vocalService?.startDetectionExternally()
        }
        if (ActivityCompat.checkSelfPermission(this, "android.permission.RECORD_AUDIO") != 0) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf("android.permission.RECORD_AUDIO"),
                101
            )
            return
        }
        initializePlayerAndStartRecording()
    }
    override fun onDestroy() {
        super.onDestroy()
        if (isBound) {
            unbindService(connection)
            isBound = false
        }
    }
    private fun initializePlayerAndStartRecording() {
        defaultPreferences?.save("StopService", "0")

        val serviceIntent = Intent(this, VocalService::class.java).apply {
            putExtra("isServiceRunning", isMyServiceRunning(VocalService::class.java))
        }

        if (!isMyServiceRunning(VocalService::class.java)) {
            try {
                startForegroundService(serviceIntent)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
    }

}