package cn.edu.bupt.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import okhttp3.*;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static cn.edu.bupt.util.UploadConstants.*;

/**
 * @Description: HttpUtil
 * @Author: czx
 * @CreateDate: 2019-05-24 14:39
 * @Version: 1.0
 */
@Slf4j
public class HttpUtil {

    private static final HashMap<String, List<Cookie>> cookieStore = new HashMap<>();

    ///创建okHttpClient对象
    private static final OkHttpClient mOkHttpClient = new OkHttpClient.Builder()
            .cookieJar(new CookieJar() {
                @Override
                public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
                    cookieStore.put(url.host(), cookies);
                }

                @Override
                public List<Cookie> loadForRequest(HttpUrl url) {
                    List<Cookie> cookies = cookieStore.get(url.host());
                    return cookies != null ? cookies : new ArrayList<>();
                }
            })
            .build();


    /**
     * 登录
     * @return session
     * @throws IOException
     */
    public static String login() throws IOException {

        cookieStore.clear();

        //请求体
        RequestBody bodyLogin = RequestBody.create(APPLICATION_JSON,LOGIN_INFO);

        //创建一个Request Request是OkHttp中访问的请求，Builder是辅助类。Response即OkHttp中的响应。
        final Request requestLogin = new Request.Builder()
                .url(LOGIN_URL)
                .header("Accept","text/plain, */*; q=0.01")
                .addHeader("Connection","keep-alive")
                .addHeader("Content-Type",APPLICATION_JSON.toString())
                .post(bodyLogin)
                .build();

        //得到一个call对象
        Response response = mOkHttpClient.newCall(requestLogin).execute();
        if(response.isSuccessful()){
            String sessionStr = cookieStore.get(HOST).get(0).toString();
            String session = sessionStr.substring(0,sessionStr.indexOf(";"));
            log.info("Login. Login session is [{}]",session);
            return session;
        }else{
            log.warn("Login failed.");
        }
        return null;
    }

    /**
     * 创建设备
     * @param deviceName
     * @param session
     * @return deviceId
     * @throws Exception
     */
    public static String createDevice(final String deviceName,final String session) throws Exception{

        //请求体
        JSONObject obj = new JSONObject();
        obj.put("name",deviceName);
        obj.put("lifeTime","NaN");
        RequestBody bodyCreate = RequestBody.create(APPLICATION_JSON, obj.toString());

        //创建一个Request Request是OkHttp中访问的请求，Builder是辅助类。Response即OkHttp中的响应。
        Request requestCreate = new Request.Builder()
                .url(CREATE_URL)
                .post(bodyCreate)
                .addHeader("Accept","application/json, text/plain, */*")
                .addHeader("Connection","keep-alive")
                .addHeader("Content-Type",APPLICATION_JSON.toString())
                .addHeader("Cookie",session)
                .build();
        //得到一个call对象
        Response response = mOkHttpClient.newCall(requestCreate).execute();
        if (response.isSuccessful()){
            String result = response.body().string();
            JsonObject jsonObject = new JsonParser().parse(result).getAsJsonObject();
            System.out.println("create scuuess");
            System.out.println(jsonObject.get("id").getAsString());
            String id = jsonObject.get("id").getAsString();
            if(id==null||id.equals("")){
                System.out.println("null id");
            }
            return id;
        }else{
            System.out.println("create failed");
            log.warn("Create failed.");
            return null;
        }

    }

    public static String findDeviceId(String deviceName,String session) throws Exception{
        Request requestCreate = new Request.Builder()
                .url(FIND_DEVICE_URL + deviceName)
                .get()
                .addHeader("Accept", "application/json, text/plain, */*")
                .addHeader("Connection", "keep-alive")
                .addHeader("Cookie", session)
                .build();
        //得到一个call对象
        Response response = mOkHttpClient.newCall(requestCreate).execute();
        if (response.isSuccessful()) {
            String result = response.body().string();
            System.out.println("findDeviceId result : "+result);
            return result;
        }else{
            System.out.println("token failed");
        }
        return null;
    }

    /**
     * 查看设备的token令牌
     * @param deviceId
     * @param session
     * @return device token
     * @throws Exception
     */
    public static String findToken(String deviceId,String session)throws Exception {

        //创建一个Request Request是OkHttp中访问的请求，Builder是辅助类。Response即OkHttp中的响应。
        Request requestCreate = new Request.Builder()
                .url(FIND_URL + deviceId)
                .get()
                .addHeader("Accept", "application/json, text/plain, */*")
                .addHeader("Connection", "keep-alive")
                .addHeader("Cookie", session)
                .build();
        //得到一个call对象
        Response response = mOkHttpClient.newCall(requestCreate).execute();
        if (response.isSuccessful()) {
            String result = response.body().string();
            JsonObject jsonObject = new JsonParser().parse(result).getAsJsonObject();
            String deviceToken = jsonObject.get("deviceToken").getAsString();
            System.out.println("token received");
            return deviceToken;
        }else{
            System.out.println("token failed");
        }
        return null;
    }
}
