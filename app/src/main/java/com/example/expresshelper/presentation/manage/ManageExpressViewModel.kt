package com.example.expresshelper.presentation.manage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expresshelper.domain.model.ExpressInfo
import com.example.expresshelper.domain.model.ExpressStatus
import com.example.expresshelper.domain.repository.ExpressRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ManageExpressViewModel @Inject constructor(
    private val expressRepository: ExpressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManageUiState())
    val uiState: StateFlow<ManageUiState> = _uiState.asStateFlow()

    // 所有快递
    private val _allExpress = MutableStateFlow<List<ExpressInfo>>(emptyList())
    val allExpress: StateFlow<List<ExpressInfo>> = _allExpress.asStateFlow()

    // 当前筛选状态
    private val _filterStatus = MutableStateFlow<ExpressStatus?>(null)
    val filterStatus: StateFlow<ExpressStatus?> = _filterStatus.asStateFlow()

    init {
        loadAllExpress()
    }

    private fun loadAllExpress() {
        viewModelScope.launch {
            expressRepository.getAllExpressForManage().collect { list ->
                _allExpress.value = list
                applyFilter()
            }
        }
    }

    fun setFilter(status: ExpressStatus?) {
        _filterStatus.value = status
        applyFilter()
    }

    private fun applyFilter() {
        val status = _filterStatus.value
        val all = _allExpress.value
        val filtered = if (status == null) {
            all
        } else {
            all.filter { it.status == status }
        }
        _uiState.value = _uiState.value.copy(
            expressList = filtered,
            filterStatus = status
        )
    }

    fun markAsPicked(expressId: Long) {
        viewModelScope.launch {
            expressRepository.markAsPicked(expressId)
        }
    }

    fun markAsPending(expressId: Long) {
        viewModelScope.launch {
            expressRepository.markAsPending(expressId)
        }
    }

    fun deleteExpress(expressId: Long) {
        viewModelScope.launch {
            expressRepository.deleteById(expressId)
        }
    }
}

data class ManageUiState(
    val expressList: List<ExpressInfo> = emptyList(),
    val filterStatus: ExpressStatus? = null,
    val isLoading: Boolean = false
)
