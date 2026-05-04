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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        observeData()
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