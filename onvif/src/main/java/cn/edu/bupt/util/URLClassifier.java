
package cn.edu.bupt.util;

import java.util.regex.Pattern;

/**
 * @Description: URLClassifier
 * @Author: czx
 * @CreateDate: 2019-06-11 11:08
 * @Version: 1.0
 */
public class URLClassifier {
    /*
    判断IPv4地址的正则表达式
     */
    private static final Pattern IPV4_REGEX = Pattern.compile("^(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)(\\.(25[0-5]|2[0-4]\\d|[0-1]?\\d?\\d)){3}$");

    /*
    判断标准IPv6地址的正则表达式
     */
    private static final Pattern IPV6_STD_REGEX = Pattern.compile("^(?:[0-9a-fA-F]{1,4}:){7}[0-9a-fA-F]{1,4}$");

    /*
    判断一般情况压缩的IPv6正则表达式
     */
    private static final Pattern IPV6_COMPRESS_REGEX = Pattern.compile("^((?:[0-9A-Fa-f]{1,4}(:[0-9A-Fa-f]{1,4})*)?)::((?:([0-9A-Fa-f]{1,4}:)*[0-9A-Fa-f]{1,4})?)$");

    /*
    抽取特殊的边界压缩情况

    由于IPv6压缩规则是必须要大于等于2个全0块才能压缩，不合法地址不能通过一般情况
    的压缩正则表达式IPV6_COMPRESS_REGEX判断出其不合法。所以定义了如下专用于判
    断边界特殊压缩的正则表达式。 (边界特殊压缩：开头或末尾为两个全0块，该压缩由于
    处于边界，且只压缩了2个全0块，不会导致':'数量变少)。
     */
    private static final Pattern IPV6_COMPRESS_REGEX_BORDER =
            Pattern.compile(
                    "^(::(?:[0-9A-Fa-f]{1,4})(?::[0-9A-Fa-f]{1,4}){5})|((?:[0-9A-Fa-f]{1,4})(?::[0-9A-Fa-f]{1,4}){5}::)$");

    /**
     * 判断是否为合法IPv4地址
     * @param input URL地址
     * @return 判断结果
     */
    public static boolean isIPv4Address(final String input){
        return IPV4_REGEX.matcher(input).matches();
    }

    /**
     * 判断是否为合法IPv6地址
     * @param input URL地址
     * @return 判断结果
     */
    public static boolean isIPv6Address(final String input) {
        int NUM = 0;
        for(int i = 0;i<input.length();i++){
            if(input.charAt(i) == ':')NUM++;
        }
        if(NUM > 7) return false;
        if(IPV6_STD_REGEX.matcher(input).matches()){
            return true;
        }
        if(NUM == 7){
            return IPV6_COMPRESS_REGEX_BORDER.matcher(input).matches();
        }
        else{
            return  IPV6_COMPRESS_REGEX.matcher(input).matches();
        }
    }
}
