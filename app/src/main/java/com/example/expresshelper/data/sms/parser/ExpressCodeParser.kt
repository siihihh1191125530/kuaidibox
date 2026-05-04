package com.example.expresshelper.data.sms.parser

import com.example.expresshelper.core.util.KeywordsPreferences
import com.example.expresshelper.domain.model.ExpressInfo
import com.example.expresshelper.domain.model.ExpressStatus
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 快递短信解析器
 * 支持用户自定义关键字提取取件码
 */
@Singleton
class ExpressCodeParser @Inject constructor(
    private val keywordsPreferences: KeywordsPreferences
) {

    // 备用取件码提取规则
    private val codePatterns = listOf(
        Regex("""取件码[：:]\s*([A-Za-z0-9]{4,8})"""),
        Regex("""快递码[：:]\s*([A-Za-z0-9]{4,8})"""),
        Regex("""取件码[【\[]([A-Za-z0-9]{4,8})[】\]]"""),
        Regex("""取件码\s+(\d{6,8})"""),
        Regex("""取件码是\s*(\d{4,8})"""),
        Regex("""提取码[：:]\s*([A-Za-z0-9]{4,8})"""),
        Regex("""取件密码[：:]\s*([A-Za-z0-9]{4,8})"""),
        Regex("""请凭\s*([A-Za-z0-9]{4,8})\s*取件"""),
        Regex("""凭码取件[：:]\s*([A-Za-z0-9]{4,8})""")
    )

    // 驿站品牌模式（支持2-6字）
    private val stationPatterns = listOf(
        Regex("""菜鸟驿站|菜鸟裹裹"""),
        Regex("""丰巢快递柜|丰巢柜|丰巢"""),
        Regex("""妈妈驿站"""),
        Regex("""兔喜超市|兔喜驿站"""),
        Regex("""万驿站|享驿驿站|橙递驿站|喵驿站"""),
        Regex("""[\u4e00-\u9fa5]{2,4}驿站"""),  // 通配：XX驿站
        Regex("""快递柜|智能柜|自提柜""")
    )

    // 快递公司模式（支持2-6字）
    private val courierPatterns = listOf(
        Regex("""顺丰[速运]?|顺丰快递"""),
        Regex("""京东物流|京东快递|京东"""),
        Regex("""韵达[快递快运]?|韵达"""),
        Regex("""圆通[速递]?|圆通"""),
        Regex("""中通[速递]?|中通"""),
        Regex("""申通[速递]?|申通"""),
        Regex("""极兔[速递快递]?|极兔"""),
        Regex("""德邦[快递物流]?|德邦"""),
        Regex("""中国邮政|邮政快递|EMS"""),
        Regex("""天天[速递快递]?|天天"""),
        Regex("""百世[快递]?|百世"""),
        Regex("""邮政""")
    )

    /**
     * 解析短信内容，提取快递信息
     */
    fun parseSms(sender: String, body: String, timestamp: Long): ExpressInfo? {
        // 1. 检查是否为快递短信
        if (!isExpressSms(body)) return null

        // 2. 提取取件码
        val code = extractCodeAfterKeyword(body) ?: return null

        // 3. 提取快递来源
        val source = extractSource(body)

        // 4. 提取取件地点
        val location = extractLocation(body)

        return ExpressInfo(
            code = code,
            source = source,
            location = location,
            smsSender = sender,
            smsBody = body,
            receivedTime = timestamp,
            status = ExpressStatus.PENDING
        )
    }

    /**
     * 判断是否为快递短信（使用用户自定义关键字）
     */
    private fun isExpressSms(body: String): Boolean {
        val keywords = keywordsPreferences.getAllKeywords()
        return keywords.any { keyword ->
            body.contains(keyword, ignoreCase = true)
        }
    }

    /**
     * 从关键字后面提取取件码
     * 提取关键字后面连续的字母数字和连字符
     */
    private fun extractCodeAfterKeyword(body: String): String? {
        val keywords = keywordsPreferences.getAllKeywords()

        for (keyword in keywords) {
            val index = body.indexOf(keyword, ignoreCase = true)
            if (index == -1) continue

            // 获取关键字后面的内容
            val afterKeyword = body.substring(index + keyword.length).trim()

            // 提取连续的字母、数字和连字符
            val match = Regex("""^([A-Za-z0-9][-A-Za-z0-9]*)""").find(afterKeyword)
            if (match != null) {
                return match.value.uppercase()
            }
        }
        return null
    }

    /**
     * 提取快递来源 - 分层精准提取
     * 优先级：用户自定义驿站品牌 > 预定义驿站 > 预定义快递公司 > 地址中驿站 > 关键字上下文
     */
    private fun extractSource(body: String): String {
        // 0. 优先匹配用户自定义驿站品牌
        for (brand in keywordsPreferences.getCustomStationBrands()) {
            if (body.contains(brand)) {
                return brand
            }
        }

        // 1. 驿站品牌匹配
        for (pattern in stationPatterns) {
            pattern.find(body)?.let { return it.value }
        }

        // 2. 快递公司匹配
        for (pattern in courierPatterns) {
            pattern.find(body)?.let { return it.value }
        }

        // 3. 地址中提取驿站
        Regex("""[\u4e00-\u9fa5]{2,4}市[\u4e00-\u9fa5]{2,4}[区县的][\u4e00-\u9fa5]+驿站""")
            .find(body)
            ?.let { match ->
                Regex("""[\u4e00-\u9fa5]{2,6}驿站""").find(match.value)?.let {
                    return it.value
                }
            }

        // 4. 用户关键字上下文（1-6字）
        extractSourceFromKeywordContext(body)?.let { return it }

        // 5. 默认
        return "快递"
    }

    /**
     * 从关键字上下文提取来源（保底）
     */
    private fun extractSourceFromKeywordContext(body: String): String? {
        val keywords = keywordsPreferences.getCustomKeywords()

        for (keyword in keywords) {
            val index = body.indexOf(keyword, ignoreCase = true)
            if (index == -1) continue

            val afterKeyword = body.substring(index + keyword.length)

            // 提取1-6个连续的中文字符
            val match = Regex("""^([\u4e00-\u9fa5]{1,6})""").find(afterKeyword)
            if (match != null) {
                return match.value
            }

            return keyword
        }
        return null
    }

    /**
     * 提取取件地点
     */
    private fun extractLocation(body: String): String? {
        val patterns = listOf(
            Regex("""(?:驿站|网点|快递柜|柜子)[：:]\s*(.+?)(?:\n|$)"""),
            Regex("""地址[：:]\s*(.+?)(?:\n|$)"""),
            Regex("""位于(.+?)(?:的|,|，)""")
        )

        for (pattern in patterns) {
            val match = pattern.find(body)
            if (match != null) {
                return match.groupValues[1].trim().take(50)
            }
        }
        return null
    }
}