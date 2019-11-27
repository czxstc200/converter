package cn.edu.bupt.util;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
public class DirUtil {

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

    public static boolean judgeDirExists(String filename){
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

    public static String generateFilenameByDate(){
        SimpleDateFormat sdf=new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        Date date=new Date();
        return sdf.format(date);
    }

    public static Long getZeroTimestamp(){
        long currentTimestamps=System.currentTimeMillis();
        long oneDayTimestamps= 60*60*24*1000;
        return currentTimestamps-(currentTimestamps+60*60*8*1000)%oneDayTimestamps+oneDayTimestamps;
    }
}
