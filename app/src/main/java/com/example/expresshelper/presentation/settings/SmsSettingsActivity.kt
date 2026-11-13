package com.example.expresshelper.presentation.settings

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.expresshelper.data.sms.SmsEngine
import com.example.expresshelper.databinding.ActivitySmsSettingsBinding
import com.example.expresshelper.service.SmsSyncWorker
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SmsSettingsActivity : AppCompatActivity() {
    @Inject lateinit var engine: SmsEngine
    private lateinit var binding: ActivitySmsSettingsBinding
    private val permission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        refresh()
        if (granted && engine.has(Manifest.permission.READ_SMS)) scan()
        if (!granted) Toast.makeText(this, "权限未开启，可在系统应用设置中检查；部分安装器可能限制短信授权", Toast.LENGTH_LONG).show()
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySmsSettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.toolbar.setNavigationOnClickListener { finish() }
        binding.btnReceive.setOnClickListener { ask(Manifest.permission.RECEIVE_SMS, "允许接收新短信", "用于在短信到达时提取取件码。只保存识别为快递的短信，内容仅存于本机。") }
        binding.btnRead.setOnClickListener { ask(Manifest.permission.READ_SMS, "允许补查短信", "用于补回遗漏的快递短信。首次检查最近 7 天，之后增量补查。") }
        binding.btnNotify.setOnClickListener {
            if (Build.VERSION.SDK_INT >= 33) ask(Manifest.permission.POST_NOTIFICATIONS, "允许取件提醒", "识别到新取件码后发送提醒。关闭不影响短信识别。")
            else open(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
        }
        binding.btnScan.setOnClickListener { scan() }
        binding.btnAppSettings.setOnClickListener { open(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName"))) }
        binding.btnBattery.setOnClickListener { open(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
    }
    override fun onResume() { super.onResume(); refresh() }
    private fun ask(value: String, title: String, message: String) {
        if (engine.has(value)) { Toast.makeText(this, "权限已开启", Toast.LENGTH_SHORT).show(); return }
        MaterialAlertDialogBuilder(this).setTitle(title).setMessage(message)
            .setPositiveButton("继续") { _, _ -> permission.launch(value) }.setNegativeButton("暂不开启", null).show()
    }
    private fun refresh() {
        val receive = if (engine.has(Manifest.permission.RECEIVE_SMS)) "已授权" else "未授权"
        val read = if (engine.has(Manifest.permission.READ_SMS)) "已授权" else "未授权"
        binding.tvStatus.text = "接收新短信：$receive\n补查短信：$read\n${engine.status()}"
        binding.btnScan.isEnabled = engine.has(Manifest.permission.READ_SMS)
    }
    private fun scan() {
        if (!engine.has(Manifest.permission.READ_SMS)) return
        binding.btnScan.isEnabled = false
        binding.btnScan.text = "正在补查…"
        lifecycleScope.launch {
            try { engine.scan(); SmsSyncWorker.schedule(this@SmsSettingsActivity) }
            catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (_: Exception) { Toast.makeText(this@SmsSettingsActivity, "补查失败，请检查权限后重试", Toast.LENGTH_LONG).show() }
            finally { binding.btnScan.text = "立即补查"; refresh() }
        }
    }
    private fun open(intent: Intent) {
        try { startActivity(intent) } catch (_: android.content.ActivityNotFoundException) {
            Toast.makeText(this, "请在手机设置中打开快递盒的应用设置", Toast.LENGTH_LONG).show()
        }
    }
}
