package cn.edu.bupt.util;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * @Description: DirUtil
 * @Author: czx
 * @CreateDate: 2019-05-20 14:03
 * @Version: 1.0
 */
@Slf4j
public class DirUtil {
    /**
     * 获取文件列表
     * @param path
     * @return
     */
    public static List<String> getFileList(String path) {
        File[] files = new File(path).listFiles();
        List<String> fileList = new ArrayList<>();
        if(files==null){
            return fileList;
        }
        for (File file : files) {
            if (file.isFile()) {
                fileList.add(file.getName());
            }
        }
        return fileList;
    }

    /**
     * @Description 判断文件夹是否存在,不存在则创建
     * @author CZX
     * @date 2018/11/30 12:24
     * @param [filename]
     * @return boolean
     */
    public static boolean judeDirExists(String filename){
        File file = new File(filename);
        if (file.exists()) {
            if (file.isDirectory()) {
                log.warn("dir[{}] exists",file.getName());
                return true;
            } else {
                log.error("the same name file[{}] exists, can not create dir",file.getName());
                return false;
            }
        }else{
            log.info("dir[{}] not exists, create it",file.getName());
            return file.mkdir();
        }
    }

    /**
     * @Description 根据日期生成视频的文件名
     * @author CZX
     * @date 2018/11/30 12:30
     * @param []
     * @return java.lang.String
     */
    public static String generateFilenameByDate(){
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        Date date=new Date();
        return sdf.format(date);
    }

    /**
     * @Description 获取明天的零点时间戳
     * @author CZX
     * @date 2018/11/30 14:36
     * @param []
     * @return java.lang.Long
     */
    public static Long getZeroTimestamp(){
        long currentTimestamps=System.currentTimeMillis();
        long oneDayTimestamps= 60*60*24*1000;
        return currentTimestamps-(currentTimestamps+60*60*8*1000)%oneDayTimestamps+oneDayTimestamps;
    }
}
