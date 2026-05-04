package com.example.expresshelper.core.util

import android.content.Context
import android.content.SharedPreferences
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 自定义关键字偏好设置管理
 * 用于存储用户自定义的快递筛选关键字
 */
@Singleton
class KeywordsPreferences @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs: SharedPreferences = context.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    /**
     * 获取所有自定义关键字
     */
    fun getCustomKeywords(): Set<String> {
        val saved = prefs.getStringSet(KEY_CUSTOM_KEYWORDS, null)
        return if (saved.isNullOrEmpty()) {
            // 如果没有保存过，返回默认关键字并保存
            DEFAULT_KEYWORDS.also { saveKeywords(it) }
        } else {
            saved
        }
    }

    /**
     * 添加自定义关键字
     */
    fun addKeyword(keyword: String): Boolean {
        val current = getCustomKeywords().toMutableSet()
        val added = current.add(keyword.trim())
        saveKeywords(current)
        return added
    }

    /**
     * 移除自定义关键字
     */
    fun removeKeyword(keyword: String): Boolean {
        val current = getCustomKeywords().toMutableSet()
        val removed = current.remove(keyword)
        saveKeywords(current)
        return removed
    }

    /**
     * 清空所有自定义关键字
     */
    fun clearCustomKeywords() {
        prefs.edit().remove(KEY_CUSTOM_KEYWORDS).apply()
    }

    /**
     * 获取所有关键字（全部用户自定义，无内置）
     */
    fun getAllKeywords(): Set<String> {
        return getCustomKeywords()
    }

    private fun saveKeywords(keywords: Set<String>) {
        prefs.edit().putStringSet(KEY_CUSTOM_KEYWORDS, keywords).apply()
    }

    // ==================== 用户自定义驿站品牌 ====================

    /**
     * 获取所有自定义驿站品牌
     */
    fun getCustomStationBrands(): Set<String> {
        return prefs.getStringSet(KEY_CUSTOM_STATION_BRANDS, null) ?: emptySet()
    }

    /**
     * 添加自定义驿站品牌
     */
    fun addStationBrand(brand: String): Boolean {
        val current = getCustomStationBrands().toMutableSet()
        val added = current.add(brand.trim())
        saveStationBrands(current)
        return added
    }

    /**
     * 移除自定义驿站品牌
     */
    fun removeStationBrand(brand: String): Boolean {
        val current = getCustomStationBrands().toMutableSet()
        val removed = current.remove(brand)
        saveStationBrands(current)
        return removed
    }

    /**
     * 清空所有自定义驿站品牌
     */
    fun clearCustomStationBrands() {
        prefs.edit().remove(KEY_CUSTOM_STATION_BRANDS).apply()
    }

    private fun saveStationBrands(brands: Set<String>) {
        prefs.edit().putStringSet(KEY_CUSTOM_STATION_BRANDS, brands).apply()
    }

    /**
     * 获取所有驿站品牌（内置+用户自定义）
     */
    fun getAllStationBrands(): Set<String> {
        val custom = getCustomStationBrands()
        val all = DEFAULT_STATION_BRANDS.toMutableSet()
        all.addAll(custom)
        return all
    }

    /**
     * 获取用户自定义驿站品牌数量
     */
    fun getCustomStationBrandsCount(): Int {
        return getCustomStationBrands().size
    }

    companion object {
        private const val PREFS_NAME = "express_keywords_prefs"
        private const val KEY_CUSTOM_KEYWORDS = "custom_keywords"
        private const val KEY_CUSTOM_STATION_BRANDS = "custom_station_brands"
        private val DEFAULT_KEYWORDS = setOf("取件码", "取件码为", "取货码", "取货码为")

        // 内置驿站品牌
        val DEFAULT_STATION_BRANDS = setOf(
            "菜鸟驿站", "菜鸟裹裹",
            "丰巢快递柜", "丰巢柜", "丰巢",
            "妈妈驿站",
            "兔喜超市", "兔喜驿站",
            "万驿站", "享驿驿站", "橙递驿站", "喵驿站",
            "快递柜", "智能柜", "自提柜"
        )

        // 内置快递公司
        val DEFAULT_COURIER_COMPANIES = setOf(
            "顺丰", "顺丰速运", "顺丰快递",
            "京东", "京东物流", "京东快递",
            "韵达", "韵达快递", "韵达快运",
            "圆通", "圆通快递", "圆通速递",
            "中通", "中通快递", "中通速递",
            "申通", "申通快递", "申通速递",
            "极兔", "极兔快递", "极兔速递",
            "德邦", "德邦快递", "德邦物流",
            "中国邮政", "邮政", "邮政快递", "EMS",
            "天天快递", "天天速递",
            "百世", "百世快递"
        )
    }
}
