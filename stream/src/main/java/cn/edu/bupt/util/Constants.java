package cn.edu.bupt.util;

public class Constants {

    public final static String ROOT_DIR = "/Users/czx/Downloads/";

    public final static String RECORD_LISTENER_NAME = "Rec-lis";

    public final static String PUSH_LISTENER_NAME = "Push-lis";

    public final static String OBJECT_DETECTION_LISTENER_NAME = "CV-lis";

    public final static String OBJECT_DETECTION_URL = "http://10.112.217.199:8081/classify?threshold=0.8";

    public final static String OBJECT_DETECTION_TEMP_DIR = ROOT_DIR + "CvTemp/";

    public static String getRootDir(){
        String path = System.getProperty("RootDir");
        if(path!=null){
            return path;
        }else{
            return ROOT_DIR;
        }
    }


}
