package com.example.expresshelper.presentation.manage

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.expresshelper.R
import com.example.expresshelper.databinding.ItemManageExpressBinding
import com.example.expresshelper.domain.model.ExpressInfo
import com.example.expresshelper.domain.model.ExpressStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ManageExpressAdapter(
    private val onStatusToggle: (ExpressInfo) -> Unit,
    private val onDelete: (ExpressInfo) -> Unit
) : ListAdapter<ExpressInfo, ManageExpressAdapter.ManageViewHolder>(ManageDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ManageViewHolder {
        val binding = ItemManageExpressBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ManageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ManageViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class ManageViewHolder(
        private val binding: ItemManageExpressBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

        fun bind(expressInfo: ExpressInfo) {
            binding.apply {
                // 取件码
                tvCode.text = expressInfo.code

                // 快递来源
                tvSource.text = expressInfo.source

                // 收到日期
                tvDate.text = dateFormat.format(Date(expressInfo.receivedTime))

                // 状态
                when (expressInfo.status) {
                    ExpressStatus.PENDING -> {
                        tvStatus.text = "未取"
                        tvStatus.setTextColor(ContextCompat.getColor(root.context, R.color.colorPrimary))
                        btnToggle.text = "标记已取"
                        btnToggle.setBackgroundColor(ContextCompat.getColor(root.context, R.color.colorPrimary))
                    }
                    ExpressStatus.PICKED -> {
                        tvStatus.text = "已取"
                        tvStatus.setTextColor(ContextCompat.getColor(root.context, R.color.textSecondary))
                        btnToggle.text = "标记未取"
                        btnToggle.setBackgroundColor(ContextCompat.getColor(root.context, R.color.textSecondary))
                    }
                    ExpressStatus.EXPIRED -> {
                        tvStatus.text = "已过期"
                        tvStatus.setTextColor(ContextCompat.getColor(root.context, R.color.textHint))
                        btnToggle.text = "标记未取"
                        btnToggle.setBackgroundColor(ContextCompat.getColor(root.context, R.color.textSecondary))
                    }
                }

                // 按钮点击
                btnToggle.setOnClickListener { onStatusToggle(expressInfo) }

                // 长按删除
                root.setOnLongClickListener {
                    onDelete(expressInfo)
                    true
                }
            }
        }
    }

    class ManageDiffCallback : DiffUtil.ItemCallback<ExpressInfo>() {
        override fun areItemsTheSame(oldItem: ExpressInfo, newItem: ExpressInfo): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: ExpressInfo, newItem: ExpressInfo): Boolean {
            return oldItem == newItem
        }
    }
}
