package com.example.expresshelper.presentation.main

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.expresshelper.R
import com.example.expresshelper.databinding.ActivityMainBinding
import com.example.expresshelper.presentation.settings.SettingsActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var expressAdapter: ExpressListAdapter
    private var isDialogShowing = false
    @javax.inject.Inject lateinit var smsEngine: com.example.expresshelper.data.sms.SmsEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        observeData()
        binding.tvSmsStatus.setOnClickListener { openSmsSettings() }
        binding.btnSync.setOnClickListener { syncSms() }
        binding.btnHistory.setOnClickListener { startActivity(Intent(this, com.example.expresshelper.presentation.manage.ManageExpressActivity::class.java)) }
    }

    override fun onResume() {
        super.onResume()
        updateSmsStatus()
        if (smsEngine.has(android.Manifest.permission.READ_SMS)) syncSms()
    }
    private fun openSmsSettings() {
        startActivity(Intent(this, com.example.expresshelper.presentation.settings.SmsSettingsActivity::class.java))
    }
    private fun updateSmsStatus() {
        binding.tvSmsStatus.text = if (smsEngine.has(android.Manifest.permission.RECEIVE_SMS))
            "短信接收已授权 · 点击检查运行状态" else "开启短信自动识别 · 点击设置"
    }
    private fun syncSms() {
        if (!smsEngine.has(android.Manifest.permission.READ_SMS)) { openSmsSettings(); return }
        if (!binding.btnSync.isEnabled) return
        binding.btnSync.isEnabled = false
        binding.btnSync.text = "补查中…"
        lifecycleScope.launch {
            try { smsEngine.scan() }
            catch (e: kotlinx.coroutines.CancellationException) { throw e }
            catch (_: Exception) { showMessage("短信补查未完成，请在设置中检查权限") }
            finally { binding.btnSync.isEnabled = true; binding.btnSync.text = "补查短信"; updateSmsStatus() }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
    }

    private fun setupRecyclerView() {
        expressAdapter = ExpressListAdapter(
            onMarkPicked = { expressInfo ->
                showConfirmDialog(
                    title = "确认取件",
                    message = "确定已取走快递吗？\n取件码：${expressInfo.code}"
                ) {
                    viewModel.markAsPicked(expressInfo.id)
                }
            },
            onDelete = { expressInfo ->
                showConfirmDialog(
                    title = "删除记录",
                    message = "确定删除这条快递记录吗？"
                ) {
                    viewModel.deleteExpress(expressInfo.id)
                }
            },
            onItemClick = { expressInfo ->
                showSmsContentDialog(expressInfo.smsBody)
            }
        )

        binding.recyclerView.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = expressAdapter
        }
    }

    private fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                // 观察快递列表
                launch {
                    viewModel.pendingExpress.collect { expressList ->
                        expressAdapter.submitList(expressList)
                        binding.tvSummary.text = if (expressList.isEmpty()) "取件码自动整理，出门一眼看清" else "${expressList.size} 件快递等你领取"
                        updateEmptyView(expressList.isEmpty())
                    }
                }

                // 观察操作状态
                launch {
                    viewModel.operationState.collect { state ->
                        when (state) {
                            is OperationState.Success -> {
                                showMessage(state.message)
                                viewModel.resetOperationState()
                            }
                            is OperationState.Error -> {
                                showMessage(state.message)
                                viewModel.resetOperationState()
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }

    private fun updateEmptyView(isEmpty: Boolean) {
        binding.emptyView.visibility = if (isEmpty) android.view.View.VISIBLE else android.view.View.GONE
        binding.recyclerView.visibility = if (isEmpty) android.view.View.GONE else android.view.View.VISIBLE
    }

    private fun showConfirmDialog(title: String, message: String, onConfirm: () -> Unit) {
        MaterialAlertDialogBuilder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("确定") { _, _ -> onConfirm() }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showSmsContentDialog(smsBody: String) {
        if (isDialogShowing) return
        isDialogShowing = true

        MaterialAlertDialogBuilder(this)
            .setTitle("短信内容")
            .setMessage(smsBody)
            .setCancelable(true)
            .setPositiveButton("确定") { dialog, _ ->
                isDialogShowing = false
                dialog.dismiss()
            }
            .setOnDismissListener {
                isDialogShowing = false
            }
            .show()
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}