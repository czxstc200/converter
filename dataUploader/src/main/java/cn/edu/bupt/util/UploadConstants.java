package cn.edu.bupt.util;

import okhttp3.MediaType;

public class UploadConstants {
    public static final String HOST = "127.0.0.1";

    public static final MediaType APPLICATION_JSON = MediaType.parse("application/json; charset=utf-8");

    public static final String LOGIN_INFO = "{\"username\":\"czxstc200@gmail.com\",\"password\":\"zx19950529\"}";

    public static final String LOGIN_URL = "http://"+HOST+"/api/user/login";

    public static final String CREATE_URL = "http://"+HOST+"/api/device/create";

    public static final String FIND_URL = "http://"+HOST+"/api/device/token/";

    public static final String FIND_DEVICE_URL = "http://"+HOST+"/api/device/";
}
