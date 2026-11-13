package com.example.expresshelper.presentation.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.expresshelper.databinding.ActivitySettingsBinding
import com.example.expresshelper.databinding.DialogKeywordsManagerBinding
import com.example.expresshelper.databinding.DialogKeywordsListBinding
import com.example.expresshelper.databinding.DialogStationBrandsListBinding
import com.example.expresshelper.presentation.manage.ManageExpressActivity
import com.google.android.material.chip.Chip
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupViews()
        observeData()
    }

    private fun setupToolbar() {
        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
    }

    private fun setupViews() {
        // 清理已取快递
        binding.itemClearPicked.setOnClickListener {
            showClearPickedDialog()
        }

        // 清空所有数据
        binding.itemClearAll.setOnClickListener {
            showClearAllDialog()
        }

        // 添加关键字
        binding.itemAddKeyword.setOnClickListener {
            showAddKeywordDialog()
        }

        // 管理关键字
        binding.itemManageKeywords.setOnClickListener {
            showKeywordsListDialog()
        }

        // 添加驿站品牌
        binding.itemAddStationBrand.setOnClickListener {
            showAddStationBrandDialog()
        }

        // 管理驿站品牌
        binding.itemManageStationBrands.setOnClickListener {
            showStationBrandsListDialog()
        }

        // 无障碍权限
        binding.itemSms.setOnClickListener {
            openSmsSettings()
        }

        // 管理快递
        binding.itemManageExpress.setOnClickListener {
            startActivity(Intent(this, ManageExpressActivity::class.java))
        }

        // 使用说明
        binding.itemHelp.setOnClickListener {
            showHelpDialog()
        }

        // 关于
        binding.itemAbout.setOnClickListener {
            showAboutDialog()
        }
    }

    private fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    binding.progressBar.visibility =
                        if (state.isLoading) android.view.View.VISIBLE else android.view.View.GONE

                    state.message?.let {
                        showMessage(it)
                        viewModel.clearMessage()
                    }
                }
            }
        }
    }

    private fun showClearPickedDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("清理已取快递")
            .setMessage("确定要清理所有已取件的快递记录吗？")
            .setPositiveButton("清理") { _, _ ->
                viewModel.clearPickedExpress()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showClearAllDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("清空所有数据")
            .setMessage("确定要清空所有快递记录吗？此操作不可恢复！")
            .setPositiveButton("清空") { _, _ ->
                viewModel.clearAllExpress()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showAddKeywordDialog() {
        val dialogBinding = DialogKeywordsManagerBinding.inflate(LayoutInflater.from(this))

        MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("添加") { _, _ ->
                val keyword = dialogBinding.etKeyword.text?.toString() ?: ""
                viewModel.addKeyword(keyword)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showKeywordsListDialog() {
        val dialogBinding = DialogKeywordsListBinding.inflate(LayoutInflater.from(this))

        val keywords = viewModel.uiState.value.customKeywords

        if (keywords.isEmpty()) {
            dialogBinding.chipGroupKeywords.visibility = android.view.View.GONE
            dialogBinding.tvEmptyKeywords.visibility = android.view.View.VISIBLE
        } else {
            dialogBinding.chipGroupKeywords.visibility = android.view.View.VISIBLE
            dialogBinding.tvEmptyKeywords.visibility = android.view.View.GONE

            dialogBinding.chipGroupKeywords.removeAllViews()
            keywords.forEach { keyword ->
                val chip = Chip(this).apply {
                    text = keyword
                    isCloseIconVisible = true
                    setOnCloseIconClickListener {
                        showDeleteKeywordConfirm(keyword)
                    }
                }
                dialogBinding.chipGroupKeywords.addView(chip)
            }
        }

        MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("确定", null)
            .show()
    }

    private fun showDeleteKeywordConfirm(keyword: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("删除关键字")
            .setMessage("确定删除关键字 \"$keyword\" 吗？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.removeKeyword(keyword)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showAddStationBrandDialog() {
        val dialogBinding = DialogKeywordsManagerBinding.inflate(LayoutInflater.from(this))
        dialogBinding.etKeyword.hint = "请输入驿站品牌名称"

        MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("添加") { _, _ ->
                val brand = dialogBinding.etKeyword.text?.toString() ?: ""
                viewModel.addStationBrand(brand)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showStationBrandsListDialog() {
        val dialogBinding = DialogStationBrandsListBinding.inflate(LayoutInflater.from(this))

        val state = viewModel.uiState.value

        // 显示内置品牌（只读，无删除按钮）
        dialogBinding.chipGroupDefaultBrands.removeAllViews()
        state.defaultStationBrands.forEach { brand ->
            val chip = Chip(this).apply {
                text = brand
                isCloseIconVisible = false
                isClickable = false
            }
            dialogBinding.chipGroupDefaultBrands.addView(chip)
        }

        // 显示自定义品牌（可删除）
        val customBrands = state.customStationBrands
        if (customBrands.isEmpty()) {
            dialogBinding.chipGroupCustomBrands.visibility = android.view.View.GONE
            dialogBinding.tvEmptyCustomBrands.visibility = android.view.View.VISIBLE
        } else {
            dialogBinding.chipGroupCustomBrands.visibility = android.view.View.VISIBLE
            dialogBinding.tvEmptyCustomBrands.visibility = android.view.View.GONE

            dialogBinding.chipGroupCustomBrands.removeAllViews()
            customBrands.forEach { brand ->
                val chip = Chip(this).apply {
                    text = brand
                    isCloseIconVisible = true
                    setOnCloseIconClickListener {
                        showDeleteStationBrandConfirm(brand)
                    }
                }
                dialogBinding.chipGroupCustomBrands.addView(chip)
            }
        }

        MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .setPositiveButton("确定", null)
            .show()
    }

    private fun showDeleteStationBrandConfirm(brand: String) {
        MaterialAlertDialogBuilder(this)
            .setTitle("删除驿站品牌")
            .setMessage("确定删除驿站品牌 \"$brand\" 吗？")
            .setPositiveButton("删除") { _, _ ->
                viewModel.removeStationBrand(brand)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showAboutDialog() {
        MaterialAlertDialogBuilder(this)
            .setTitle("关于快递盒")
            .setMessage("版本: 1.0（重制版）\n\n一款简洁实用的快递管理工具，自动识别短信中的快递信息，桌面小组件快速查看待取快递。")
            .setPositiveButton("确定", null)
            .show()
    }

    private fun showHelpDialog() {
        val helpText = """
            |📱 快速开始
            |
            |1. 开启短信自动识别
            |   设置 → 短信与运行状态 → 允许接收短信
            |   开启补查后，首次会检查最近 7 天短信
            |
            |2. 添加关键字（可选）
            |   默认已内置常见关键字，如需识别其他短信可自定义添加
            |
            |3. 添加驿站品牌（可选）
            |   用于精准识别快递来源，如您的小区驿站名称
            |
            |📦 桌面小组件
            |
            |• 点击取件码区域：切换查看下一条快递
            |• 点击红色圆点：标记为已取件
            |• 点击绿色圆点：标记为未取件
            |
            |💡 小提示
            |
            |• 快递来源会自动识别：驿站名称 > 快递公司 > 地址
            |• 已取快递会在1天后自动隐藏
            |• 支持的驿站：菜鸟驿站、丰巢柜、妈妈驿站、兔喜超市等
            |• 支持的快递公司：顺丰、京东、韵达、圆通、中通、申通等
        """.trimMargin()

        MaterialAlertDialogBuilder(this)
            .setTitle("使用说明")
            .setMessage(helpText)
            .setPositiveButton("知道了", null)
            .show()
    }

    private fun openSmsSettings() {
        startActivity(Intent(this, SmsSettingsActivity::class.java))
    }

    private fun showMessage(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}
