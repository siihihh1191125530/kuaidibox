package com.example.expresshelper.presentation.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.expresshelper.core.util.KeywordsPreferences
import com.example.expresshelper.domain.repository.ExpressRepository
import com.example.expresshelper.presentation.widget.WidgetUpdater
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val expressRepository: ExpressRepository,
    private val keywordsPreferences: KeywordsPreferences,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        loadKeywords()
        loadStationBrands()
    }

    private fun loadKeywords() {
        val customKeywords = keywordsPreferences.getCustomKeywords()
        _uiState.value = _uiState.value.copy(customKeywords = customKeywords.toList())
    }

    private fun loadStationBrands() {
        val defaultBrands = KeywordsPreferences.DEFAULT_STATION_BRANDS.toList()
        val customBrands = keywordsPreferences.getCustomStationBrands().toList()
        _uiState.value = _uiState.value.copy(
            defaultStationBrands = defaultBrands,
            customStationBrands = customBrands
        )
    }

    fun clearPickedExpress() {
        viewModelScope.launch {
            val oneWeekAgo = System.currentTimeMillis()
            expressRepository.deletePickedBefore(oneWeekAgo)
            _uiState.value = _uiState.value.copy(message = "已清理已取快递记录")
            WidgetUpdater.update(context)
        }
    }

    fun clearAllExpress() {
        viewModelScope.launch {
            expressRepository.deleteAll()
            _uiState.value = _uiState.value.copy(message = "已清空所有数据")
            WidgetUpdater.update(context)
        }
    }

    fun addKeyword(keyword: String) {
        if (keyword.isBlank()) {
            _uiState.value = _uiState.value.copy(message = "关键字不能为空")
            return
        }

        val trimmed = keyword.trim()
        keywordsPreferences.addKeyword(trimmed)
        loadKeywords()
        _uiState.value = _uiState.value.copy(message = "已添加关键字: $trimmed")
    }

    fun removeKeyword(keyword: String) {
        keywordsPreferences.removeKeyword(keyword)
        loadKeywords()
        _uiState.value = _uiState.value.copy(message = "已删除关键字: $keyword")
    }

    fun addStationBrand(brand: String) {
        if (brand.isBlank()) {
            _uiState.value = _uiState.value.copy(message = "驿站品牌不能为空")
            return
        }

        val trimmed = brand.trim()
        keywordsPreferences.addStationBrand(trimmed)
        loadStationBrands()
        _uiState.value = _uiState.value.copy(message = "已添加驿站品牌: $trimmed")
    }

    fun removeStationBrand(brand: String) {
        keywordsPreferences.removeStationBrand(brand)
        loadStationBrands()
        _uiState.value = _uiState.value.copy(message = "已删除驿站品牌: $brand")
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }
}

data class SettingsUiState(
    val isLoading: Boolean = false,
    val customKeywords: List<String> = emptyList(),
    val defaultStationBrands: List<String> = emptyList(),
    val customStationBrands: List<String> = emptyList(),
    val message: String? = null
)