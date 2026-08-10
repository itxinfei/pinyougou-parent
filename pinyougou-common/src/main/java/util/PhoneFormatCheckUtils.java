package util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class PhoneFormatCheckUtils {

    /** 大陆手机号码正则 */
    private static final Pattern CHINA_PHONE_PATTERN = Pattern.compile(
            "^((13[0-9])|(14[0-9])|(15[0-9])|(16[0-9])|(17[0-9])|(18[0-9])|(19[0-9]))\\d{8}$");

    /** 香港手机号码正则 */
    private static final Pattern HK_PHONE_PATTERN = Pattern.compile("^(5|6|8|9)\\d{7}$");

    /**
     * 大陆号码或香港号码均可
     */
    public static boolean isPhoneLegal(String str) throws PatternSyntaxException {
        return isChinaPhoneLegal(str) || isHKPhoneLegal(str);
    }

    /**
     * 大陆手机号码11位数，匹配格式：前三位固定格式+后8位任意数
     * 此方法中前三位格式有：
     * 13+任意数
     * 15+除4的任意数
     * 18+除1和4的任意数
     * 17+除9的任意数
     * 147
     */
    public static boolean isChinaPhoneLegal(String str) throws PatternSyntaxException {
        return CHINA_PHONE_PATTERN.matcher(str).matches();
    }

    /**
     * 香港手机号码8位数，5|6|8|9开头+7位任意数
     */
    public static boolean isHKPhoneLegal(String str) throws PatternSyntaxException {
        return HK_PHONE_PATTERN.matcher(str).matches();
    }

}
