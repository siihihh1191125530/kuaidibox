package com.example.expresshelper.data.sms.parser

import java.text.Normalizer
import java.util.Locale

/** Pure rules shared by live reception and inbox recovery. */
object CodeRules {
    private const val CODE = "([A-Za-z0-9]+(?:-[A-Za-z0-9]+)*)(?![A-Za-z0-9-])"
    fun extract(body: String, keywords: Set<String>): List<String> {
        val text = Normalizer.normalize(body, Normalizer.Form.NFKC)
            .replace('－', '-').replace('—', '-').replace('–', '-')
        val keys = (keywords + setOf("取件码", "取货码", "提货码", "取件密码", "提取码"))
            .filter { it.isNotBlank() }.sortedByDescending { it.length }
        val labels = keys.joinToString("|") { Regex.escape(it) }
        val labelled = Regex("(?:$labels)\\s*(?:为|是)?\\s*[:：]?\\s*[【\\[（(]?\\s*$CODE", RegexOption.IGNORE_CASE)
        val result = labelled.findAll(text).map { it.groupValues[1] }.toMutableList()
        if (Regex("快递|包裹|驿站|快递柜|丰巢").containsMatchIn(text)) {
            Regex("凭\\s*[【\\[]?\\s*$CODE\\s*[】\\]]?\\s*(?:到|至|前往|取件|领取)")
                .findAll(text).forEach { result.add(it.groupValues[1]) }
        }
        return result.filter { it.length in 3..20 && it.any(Char::isDigit) &&
            !Regex("1[3-9][0-9]{9}").matches(it) &&
            (it.contains('-') || it.length <= 10) }
            .map { it.uppercase(Locale.ROOT) }.distinct()
    }
}
