package cn.edu.bupt.server.parser;

import io.netty.handler.codec.http.FullHttpRequest;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @Description: RequestParser
 * @Author: czx
 * @CreateDate: 2019-06-05 23:25
 * @Version: 1.0
 */
public class RequestParser {

    public static Map<String, String> parse(FullHttpRequest request) throws Exception {
        HttpMethod method = request.method();

        Map<String, String> paramMap = new HashMap<>();

        if (HttpMethod.GET == method) {
            // 是GET请求
            QueryStringDecoder decoder = new QueryStringDecoder(request.uri());
            decoder.parameters().entrySet().forEach( entry -> {
                // entry.getValue()是一个List, 只取第一个元素
                paramMap.put(entry.getKey(), entry.getValue().get(0));
            });
        } else if (HttpMethod.POST == method) {
            throw new Exception("Unsupported method");
//            // 是POST请求
//            HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(request);
//            decoder.offer(request);
//
//            List<InterfaceHttpData> paramList = decoder.getBodyHttpDatas();
//
//            for (InterfaceHttpData param : paramList) {
//                Attribute data = (Attribute) param;
//                paramMap.put(data.getName(), data.getValue());
//            }
        } else {
            throw new Exception("Unsupported method");
            // 不支持其它方法
        }
        return paramMap;
    }
}
