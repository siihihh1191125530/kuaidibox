package com.example.expresshelper.presentation.manage

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.expresshelper.databinding.ActivityManageExpressBinding
import com.example.expresshelper.domain.model.ExpressStatus
import com.example.expresshelper.presentation.manage.ManageExpressAdapter
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ManageExpressActivity : AppCompatActivity() {

    private lateinit var binding: ActivityManageExpressBinding
    private val viewModel: ManageExpressViewModel by viewModels()
    private lateinit var adapter: ManageExpressAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageExpressBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupFilterTabs()
        observeData()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        adapter = ManageExpressAdapter(
            onStatusToggle = { expressInfo ->
                if (expressInfo.status == ExpressStatus.PENDING) {
                    viewModel.markAsPicked(expressInfo.id)
                } else {
                    viewModel.markAsPending(expressInfo.id)
                }
            },
            onDelete = { expressInfo ->
                showDeleteConfirm(expressInfo.id, expressInfo.code)
            }
        )

        binding.recyclerView.layoutManager = LinearLayoutManager(this)
        binding.recyclerView.adapter = adapter
    }

    private fun setupFilterTabs() {
        binding.tabAll.setOnClickListener { viewModel.setFilter(null) }
        binding.tabPending.setOnClickListener { viewModel.setFilter(ExpressStatus.PENDING) }
        binding.tabPicked.setOnClickListener { viewModel.setFilter(ExpressStatus.PICKED) }
    }

    private fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    adapter.submitList(state.expressList)
                    updateEmptyView(state.expressList.isEmpty())
                    updateFilterTabs(state.filterStatus)
                }
            }
        }
    }

    private fun updateEmptyView(isEmpty: Boolean) {
        binding.emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerView.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun updateFilterTabs(status: ExpressStatus?) {
        binding.tabAll.isChecked = status == null
        binding.tabPending.isChecked = status == ExpressStatus.PENDING
        binding.tabPicked.isChecked = status == ExpressStatus.PICKED
    }

    private fun showDeleteConfirm(id: Long, code: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("删除记录")
            .setMessage("确定删除取件码 $code 的记录吗？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.deleteExpress(id)
            }
            .setNegativeButton("取消", null)
            .show()
    }
}
