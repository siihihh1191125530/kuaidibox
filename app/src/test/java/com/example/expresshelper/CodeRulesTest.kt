package com.example.expresshelper

import com.example.expresshelper.data.sms.parser.CodeRules
import org.junit.Assert.*
import org.junit.Test

class CodeRulesTest {
    private fun codes(body: String) = CodeRules.extract(body, setOf("取件码", "取件码为", "领取号"))
    @Test fun punctuationAndFullWidth() {
        assertEquals(listOf("A-12-3456"), codes("【菜鸟驿站】取件码为：【Ａ－１２－３４５６】，请及时领取"))
        assertEquals(listOf("123456"), codes("丰巢取件码是 123456"))
    }
    @Test fun multipleCodesAndRepeatedLabels() {
        assertEquals(listOf("1-2-300", "998877"), codes("取件码：1-2-300；取件码【998877】；取件码：1-2-300"))
    }
    @Test fun noOtpPhoneTrackingOrPlainText() {
        assertTrue(codes("验证码123456，请勿泄露").isEmpty())
        assertTrue(codes("取件码：13800138000").isEmpty())
        assertTrue(codes("取件码：SF1234567890123").isEmpty())
        assertTrue(codes("取件码：https://example.com").isEmpty())
        assertTrue(codes("快递已签收，客服电话123456").isEmpty())
    }
    @Test fun contextAndCustomKeyword() {
        assertEquals(listOf("D15-2560"), codes("包裹已到菜鸟驿站，请凭 D15-2560 到店领取"))
        assertEquals(listOf("8822"), codes("领取号：8822"))
        assertTrue(codes("凭123456到店支付").isEmpty())
    }
}
