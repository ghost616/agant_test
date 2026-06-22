package com.datanew.core.toolkit;

import com.datanew.core.lang.PatternPool;
import com.datanew.core.lang.ValidateException;
import org.junit.Test;

import static org.junit.Assert.*;

public class ValidateKitTest {

    @Test
    public void isNull() {
        // 给定值是否为null
        assertFalse(ValidateKit.isNull("123"));
    }

    @Test
    public void isNotNull() {
        // 给定值是否不为null
        assertTrue(ValidateKit.isNotNull(111));
    }

    @Test
    public void validateNotNull() {
        // 检查指定值是否为null  errorMsgTemplate 错误消息内容模板（变量使用{}表示） params 模板中变量替换后的值
        Object ret = ValidateKit.validateNotNull("123", "{}为空", "Object");
    }

    @Test
    public void isEmpty() {
        // 验证是否为空  对于String类型判定是否为empty(null 或 "")
        assertTrue(ValidateKit.isEmpty(""));
        assertTrue(ValidateKit.isEmpty(null));
    }

    @Test
    public void isNotEmpty() {
        // 验证是否不为空
        assertTrue(ValidateKit.isNotEmpty("123"));
    }

    @Test
    public void validateNotEmpty() {
        // 验证是否为空，为空时抛出异常
        try {
            ValidateKit.validateNotEmpty("", "值为空");
        } catch (ValidateException e) {
            System.out.println(e);
        }


    }

    @Test
    public void equal() {
        // 验证是否相等
        assertTrue(ValidateKit.equal(null, null));
        assertFalse(ValidateKit.equal(1, 23));
    }

    @Test
    public void validateEqual() {
        // 验证是否相等，不相等则输出“值不相等”
        try {
            ValidateKit.validateEqual(null, 1, "值不相等");
        } catch (ValidateException e) {
            System.out.println(e);
        }

    }

    @Test
    public void validateNotEqual() {
        // 验证是否不等，相等则输出“值相等”
        try {
            ValidateKit.validateNotEqual(null, null, "值相等");
        } catch (ValidateException e) {
            System.out.println(e);
        }
    }

    @Test
    public void validateNotEmptyAndEqual() {
        // 验证是否非空且与指定值相等
        try {
            ValidateKit.validateNotEmptyAndEqual(1, 2, "值为空或者值不相等");
        } catch (ValidateException e) {
            System.out.println(e);
        }
    }

    @Test
    public void validateNotEmptyAndNotEqual() {
        // 验证是否非空且与指定值相等
        try {
            ValidateKit.validateNotEmptyAndNotEqual(2, 2, "值为空或者值相等");
        } catch (ValidateException e) {
            System.out.println(e);
        }
    }

    @Test
    public void isMatchRegex() {
        // 通过正则表达式验证
        assertTrue(ValidateKit.isMatchRegex(ValidateKit.MONEY, "123.21"));
    }

    @Test
    public void validateMatchRegex() {
        // 通过正则表达式验证，不符合正则则输出错误信息
        try {
            ValidateKit.validateMatchRegex("\\d+", "1234s", "正则不匹配");
            ValidateKit.validateMatchRegex("^(\\d+(?:\\.\\d+)?)$", "123.21", "正则不匹配");
        } catch (ValidateException e) {
            System.out.println(e);
        }
    }

    @Test
    public void testIsMatchRegex() {
        //通过正则表达式验证
        assertTrue(ValidateKit.isMatchRegex(PatternPool.IPV4, "192.168.1.1"));

    }

    @Test
    public void testIsGeneral() {
        //  验证是否为英文字母 、数字和下划线
        assertTrue(ValidateKit.isGeneral("adf_123"));
        // 验证是否为给定长度范围的英文字母 、数字和下划线
        assertTrue(ValidateKit.isGeneral("321_a", 2, 6));
        // 验证是否为给定最小长度的英文字母 、数字和下划线
        assertTrue(ValidateKit.isGeneral("_a", 2));
    }

    @Test
    public void testValidateGeneral() {
        try {
            // 验证是否为英文字母 、数字和下划线
            ValidateKit.validateGeneral("adf_123", "只能是字母数字下划线组成");
            // 验证是否为给定长度范围的英文字母 、数字和下划线
            ValidateKit.validateGeneral("321_a", 2, 6, "只能由字母数字下划线组成或者超出范围");
            // 验证是否为给定最小长度的英文字母 、数字和下划线
            ValidateKit.validateGeneral("_a", 2, "只能由字母数字下划线组成或者低于最小长度");
        } catch (ValidateException e) {
            System.out.println(e);
        }

    }

    @Test
    public void isLetter() {
        // 判断字符串是否全部为字母组成，包括大写和小写字母和汉字
        assertTrue(ValidateKit.isLetter("ABCabc哈哈"));
        assertFalse(ValidateKit.isLetter("_ABCabc哈哈"));
    }

    @Test
    public void validateLetter() {
        // 验证是否全部为字母组成，包括大写和小写字母和汉字
        try {
            ValidateKit.validateLetter("ABCabc哈哈", "字符串只能由字母组成，包括大写和小写字母和汉字");
            ValidateKit.validateLetter("ABCabc哈哈1", "字符串只能由字母组成，包括大写和小写字母和汉字");
        } catch (ValidateException e) {
            System.out.println(e);
        }

    }

    @Test
    public void isUpperCase() {
        // 判断字符串是否全部为大写字母
        assertTrue(ValidateKit.isUpperCase("ABC"));
        assertFalse(ValidateKit.isUpperCase("abC"));
    }

    @Test
    public void validateUpperCase() {
        // 验证字符串是否全部为大写字母
        try {
            ValidateKit.validateUpperCase("ABC", "不是全部大写");
            ValidateKit.validateUpperCase("ABc", "不是全部大写");
        } catch (ValidateException e) {
            System.out.println(e);
        }

    }

    @Test
    public void isLowerCase() {
        // 判断字符串是否全部为小写字母
        assertFalse(ValidateKit.isLowerCase("ABC"));
        assertTrue(ValidateKit.isLowerCase("abc"));
    }

    @Test
    public void validateLowerCase() {
        // 验证字符串是否全部为小写字母
        try {
            ValidateKit.validateLowerCase("ABC", "不是全部小写");
        } catch (ValidateException e) {
            System.out.println(e);
        }
    }

    @Test
    public void isNumber() {
        // 验证该字符串是否是数字
        assertFalse(ValidateKit.isNumber("ABC"));
        assertTrue(ValidateKit.isNumber("123"));
    }

    @Test
    public void validateNumber() {
        // 验证是否为数字
        try {
            ValidateKit.validateNumber("123", "不是全部数字");
        } catch (ValidateException e) {
            System.out.println(e);
        }
    }

    @Test
    public void isWord() {
        // 验证该字符串是否是字母（包括大写和小写字母）
        assertFalse(ValidateKit.isWord("123"));
        assertTrue(ValidateKit.isWord("ABCd"));
    }

    @Test
    public void isLowerWord() {
        // 验证该字符串是否是字母（小写字母）
        assertFalse(ValidateKit.isLowerWord("ABCd"));
        assertTrue(ValidateKit.isLowerWord("abcd"));
    }

    @Test
    public void isUpperWord() {
        // 验证该字符串是否是字母（大写字母）
        assertTrue(ValidateKit.isUpperWord("ABCD"));
        assertFalse(ValidateKit.isUpperWord("abcd"));
    }

    @Test
    public void validateWord() {
        // 验证是否为字母（包括大写和小写字母）
        try {
            ValidateKit.validateWord("Abc", "不是全部字母");
        } catch (ValidateException e) {
            System.out.println(e);
        }
    }

    @Test
    public void isMoney() {
        // 验证是否为货币
        assertTrue(ValidateKit.isMoney("123.32"));
        assertFalse(ValidateKit.isMoney("abcd"));
    }

    @Test
    public void validateMoney() {
        // 验证是否为货币 不是则输出错误信息
        try {
            ValidateKit.validateMoney("123.32", "不是货币");
        } catch (ValidateException e) {
            System.out.println(e);
        }
    }

    @Test
    public void isZipCode() {
        // 验证是否为邮政编码（中国）
        assertTrue(ValidateKit.isZipCode("311202"));
        assertFalse(ValidateKit.isZipCode("311231232"));
    }

    @Test
    public void validateZipCode() {
        // 验证是否为邮政编码（中国） 不是则输出错误信息
        try {
            ValidateKit.validateZipCode("311202", "不是中国邮政编码");
        } catch (ValidateException e) {
            System.out.println(e);
        }
    }

    @Test
    public void isEmail() {
        // 验证是否为可用邮箱地址
        assertTrue(ValidateKit.isEmail("356897457@qq.com"));
        assertFalse(ValidateKit.isEmail("@356897457@qq.com"));
    }

    @Test
    public void validateEmail() {
        // 验证是否为可用邮箱地址 不是则输出错误信息
        try {
            ValidateKit.validateEmail("356897457@qq.com", "不是可用邮箱");
        } catch (ValidateException e) {
            System.out.println(e);
        }
    }

    @Test
    public void isMobile() {
        //  验证是否为手机号码（中国）
        assertTrue(ValidateKit.isMobile("13567458585"));
        assertFalse(ValidateKit.isMobile("123"));
    }

    @Test
    public void validateMobile() {
        // //  验证是否为手机号码（中国）不是则输出错误信息
        try {
            ValidateKit.validateMobile("13567458585@qq.com", "不是中国的手机号");
        } catch (ValidateException e) {
            System.out.println(e);
        }
    }

    @Test
    public void isCitizenId() {
        // 验证是否为身份证号码（18位中国）
        //	 * 出生日期只支持到到2999年
        assertTrue(ValidateKit.isCitizenId("339005199204203123"));
        assertFalse(ValidateKit.isCitizenId("123"));
    }

    @Test
    public void validateCitizenIdNumber() {
        // 验证是否为身份证号码（18位中国） 不是则输出错误信息
        try {
            ValidateKit.validateCitizenIdNumber("339005199204203123", "不是中国的身份证号");
        } catch (ValidateException e) {
            System.out.println(e);
        }
    }

    @Test
    public void isBirthday() {
        // 验证是否为生日
        assertTrue(ValidateKit.isBirthday(2020, 01, 03));
        assertFalse(ValidateKit.isBirthday(2020, 13, 13));
        assertTrue(ValidateKit.isBirthday("20200103"));
        assertTrue(ValidateKit.isBirthday("2020-01-03"));
        assertTrue(ValidateKit.isBirthday("2020/01/03"));
        assertTrue(ValidateKit.isBirthday("2020年01月03日"));
    }

    @Test
    public void validateBirthday() {
        // 验证是否为生日 不是则输出错误信息
        try {
            ValidateKit.validateBirthday("20200103", "不是生日");
            ValidateKit.validateBirthday("2020-01-03", "不是生日");
            ValidateKit.validateBirthday("2020/01/03", "不是生日");
            ValidateKit.validateBirthday("2020年01月03日", "不是生日");
        } catch (ValidateException e) {
            System.out.println(e);
        }
    }

    @Test
    public void isIpv4() {
        // 验证是否为IPV4地址
        assertTrue(ValidateKit.isIpv4("192.0.0.0"));
        assertFalse(ValidateKit.isIpv4("192.0.0.0.1"));

    }

    @Test
    public void validateIpv4() {
        // 验证是否为IPV4地址 不是则输出错误信息
        try {
            ValidateKit.validateIpv4("192.0.0.0", "不是ipv4地址");
        } catch (ValidateException e) {
            System.out.println(e);
        }
    }

    @Test
    public void isMac() {
        // 验证是否为MAC地址
        assertTrue(ValidateKit.isMac("18-31-BF-07-EA-5E"));
        assertFalse(ValidateKit.isMac("192.0.0.0.1"));
    }

    @Test
    public void validateMac() {
        // 验证是否为MAC地址 不是则输出错误信息
        try {
            ValidateKit.validateMac("18-31-BF-07-EA-5E", "不是mac地址");
        } catch (ValidateException e) {
            System.out.println(e);
        }
    }

    @Test
    public void isPlateNumber() {
        // 验证是否为中国车牌号
        assertTrue(ValidateKit.isPlateNumber("浙A1233Q"));
    }

    @Test
    public void validatePlateNumber() {
        // 验证是否为中国车牌号 不是则输出错误信息
        try {
            ValidateKit.validatePlateNumber("浙A1233Q", "不是中国车牌号");
        } catch (ValidateException e) {
            System.out.println(e);
        }
    }

    @Test
    public void isUrl() {
        // 验证是否为URL
        assertTrue(ValidateKit.isUrl("http://www.baidu.com"));
    }

    @Test
    public void validateUrl() {
        // 验证是否为URL
        // 不是则输出错误信息
        try {
            ValidateKit.validateUrl("http://www.baidu.com", "不是url");
        } catch (ValidateException e) {
            System.out.println(e);
        }
    }

    @Test
    public void isChinese() {
        // 验证是否为汉字
        assertTrue(ValidateKit.isChinese("中文"));
    }

    @Test
    public void isIncludeChinese() {
        // 验证是否包含汉字
        assertTrue(ValidateKit.isIncludeChinese("include中文"));

    }

    @Test
    public void validateChinese() {
        // 验证是否为汉字 不是则输出错误信息
        try {
            ValidateKit.validateChinese("abac", "不是中文");
        } catch (ValidateException e) {
            System.out.println(e);
        }
    }

    @Test
    public void isGeneralWithChinese() {
        // 验证是否为中文字、英文字母、数字和下划线
        assertTrue(ValidateKit.isGeneralWithChinese("include_123_中文"));
    }

    @Test
    public void validateGeneralWithChinese() {
        // 验证是否为中文字、英文字母、数字和下划线 不是则输出错误信息
        try {
            ValidateKit.validateGeneralWithChinese("abac", "不是中文字、英文字母、数字和下划线组成");
        } catch (ValidateException e) {
            System.out.println(e);
        }
    }

    @Test
    public void isUUID() {
        // 验证是否为UUID
        assertTrue(ValidateKit.isUUID("a07058ee-fac5-4127-85fe-58cbd939c463"));
    }

    @Test
    public void validateUUID() {
        // 验证是否为UUID 不是则输出错误信息
        try {
            ValidateKit.validateUUID("abac", "不是UUID");
        } catch (ValidateException e) {
            System.out.println(e);
        }
    }

    @Test
    public void isHex() {
        // 验证是否为Hex（16进制）字符串
        assertTrue(ValidateKit.isHex("647361647361"));
    }

    @Test
    public void validateHex() {
        // 验证是否为Hex（16进制）字符串 不是则输出错误信息
        try {
            ValidateKit.validateHex("647361647361", "不是Hex（16进制）字符串");
        } catch (ValidateException e) {
            System.out.println(e);
        }
    }

    @Test
    public void isBetween() {
        // 检查给定的数字是否在指定范围内
        assertTrue(ValidateKit.isBetween(3,1,10));
    }

    @Test
    public void validateBetween() {
        // 检查给定的数字是否在指定范围内 不是则输出错误信息
        try {
            ValidateKit.validateBetween(3,1,10, "不在指定范围内");
        } catch (ValidateException e) {
            System.out.println(e);
        }
    }
}