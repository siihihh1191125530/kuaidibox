package com.example.expresshelper.data.sms.parser

import com.example.expresshelper.core.util.KeywordsPreferences
import com.example.expresshelper.domain.model.ExpressInfo
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ExpressCodeParser @Inject constructor(private val preferences: KeywordsPreferences) {
    fun parseSms(sender: String, body: String, timestamp: Long): ExpressInfo? = parseAll(sender, body, timestamp).firstOrNull()
    fun parseAll(sender: String, body: String, timestamp: Long): List<ExpressInfo> {
        val codes = CodeRules.extract(body, preferences.getAllKeywords())
        if (codes.isEmpty()) return emptyList()
        val source = (preferences.getCustomStationBrands().sortedByDescending { it.length } +
            KeywordsPreferences.DEFAULT_STATION_BRANDS.sortedByDescending { it.length } +
            KeywordsPreferences.DEFAULT_COURIER_COMPANIES.sortedByDescending { it.length })
            .firstOrNull { body.contains(it, ignoreCase = true) } ?: "快递"
        val location = Regex("""(?:取件地址|地址|地点)[：:]\s*([^，。；;\n]{2,60})""").find(body)?.groupValues?.get(1)
            ?: Regex("""(?:请到|请至|已到|放在)\s*([^，。；;\n]{2,40}?(?:驿站|快递柜|超市|便利店))""").find(body)?.groupValues?.get(1)
        return codes.map { code -> ExpressInfo(code = code, source = source, location = location?.trim(),
            smsSender = sender, smsBody = body, receivedTime = timestamp) }
    }
}
