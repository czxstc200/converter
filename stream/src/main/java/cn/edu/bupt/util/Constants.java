package cn.edu.bupt.util;

/**
 * @Description: cn.edu.bupt.util.Constants
 * @Author: czx
 * @CreateDate: 2018-12-07 14:14
 * @Version: 1.0
 */
public class Constants {

//    public final static String ROOT_DIR = "/home/rec/";
    public final static String ROOT_DIR = "/Users/czx/Downloads/";

    public final static String RECORD_LISTENER_NAME = "Rec-lis";

    public final static String PUSH_LISTENER_NAME = "Push-lis";

    public final static String getRootDir(){
        String path = System.getProperty("RootDir");
        if(path!=null){
            return path;
        }else{
            return ROOT_DIR;
        }
    }


}
