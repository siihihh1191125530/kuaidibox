package com.example.expresshelper.presentation.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expresshelper.domain.model.ExpressInfo
import com.example.expresshelper.domain.repository.ExpressRepository
import com.example.expresshelper.presentation.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val expressRepository: ExpressRepository
) : ViewModel() {

    // 待取快递列表
    val pendingExpress: StateFlow<List<ExpressInfo>> = expressRepository
        .getPendingExpress()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 待取快递数量
    val pendingCount: StateFlow<Int> = expressRepository
        .getPendingCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    // 操作状态
    private val _operationState = MutableStateFlow<OperationState>(OperationState.Idle)
    val operationState: StateFlow<OperationState> = _operationState.asStateFlow()

    /**
     * 标记快递已取
     */
    fun markAsPicked(expressId: Long) {
        viewModelScope.launch {
            try {
                _operationState.value = OperationState.Loading
                expressRepository.markAsPicked(expressId)
                _operationState.value = OperationState.Success("已标记为取件完成")
            } catch (e: Exception) {
                _operationState.value = OperationState.Error(e.message ?: "操作失败")
            }
        }
    }

    /**
     * 删除快递记录
     */
    fun deleteExpress(expressId: Long) {
        viewModelScope.launch {
            try {
                expressRepository.deleteById(expressId)
            } catch (e: Exception) {
                // 忽略删除错误
            }
        }
    }

    /**
     * 重置操作状态
     */
    fun resetOperationState() {
        _operationState.value = OperationState.Idle
    }
}

sealed class OperationState {
    object Idle : OperationState()
    object Loading : OperationState()
    data class Success(val message: String) : OperationState()
    data class Error(val message: String) : OperationState()
}
