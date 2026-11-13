package com.example.expresshelper.presentation.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.expresshelper.R
import com.example.expresshelper.databinding.ItemExpressBinding
import com.example.expresshelper.domain.model.ExpressInfo
import com.example.expresshelper.domain.model.ExpressStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ExpressListAdapter(
    private val onMarkPicked: (ExpressInfo) -> Unit,
    private val onDelete: (ExpressInfo) -> Unit,
    private val onItemClick: (ExpressInfo) -> Unit
) : ListAdapter<ExpressInfo, ExpressListAdapter.ExpressViewHolder>(ExpressDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExpressViewHolder {
        val binding = ItemExpressBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ExpressViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ExpressViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ExpressViewHolder(
        private val binding: ItemExpressBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val timeFormat = SimpleDateFormat("MM-dd HH:mm", Locale.getDefault())

        fun bind(expressInfo: ExpressInfo) {
            // 设置状态指示圆点颜色
            val statusColor = when (expressInfo.status) {
                ExpressStatus.PICKED -> "#4CAF50"   // 绿色 - 已取
                ExpressStatus.EXPIRED -> "#FF9800" // 橙色 - 超期
                else -> "#F44336"                   // 红色 - 未取
            }
            binding.statusIndicator.setBackgroundResource(R.drawable.status_circle)
            binding.statusIndicator.background.setTint(android.graphics.Color.parseColor(statusColor))

            // 取件码 - 黑字
            binding.tvCode.text = expressInfo.code

            // 快递来源 - 不显示关键字，隐藏
            binding.tvSource.visibility = android.view.View.VISIBLE
            binding.tvSource.text = expressInfo.source

            // 收件时间
            binding.tvTime.text = timeFormat.format(Date(expressInfo.receivedTime))

            // 取件地点
            if (!expressInfo.location.isNullOrEmpty()) {
                binding.tvLocation.text = expressInfo.location
                binding.tvLocation.visibility = android.view.View.VISIBLE
            } else {
                binding.tvLocation.visibility = android.view.View.GONE
            }

            binding.tvCode.setOnClickListener {
                val clipboard = binding.root.context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("取件码", expressInfo.code))
                android.widget.Toast.makeText(binding.root.context, "取件码已复制", android.widget.Toast.LENGTH_SHORT).show()
            }
            // 点击查看短信内容
            binding.root.setOnClickListener {
                onItemClick(expressInfo)
            }

            // 标记已取按钮
            binding.btnMarkPicked.setOnClickListener {
                onMarkPicked(expressInfo)
            }

            // 长按删除
            binding.root.setOnLongClickListener {
                onDelete(expressInfo)
                true
            }
        }
    }

    class ExpressDiffCallback : DiffUtil.ItemCallback<ExpressInfo>() {
        override fun areItemsTheSame(oldItem: ExpressInfo, newItem: ExpressInfo): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ExpressInfo, newItem: ExpressInfo): Boolean {
            return oldItem == newItem
        }
    }
}