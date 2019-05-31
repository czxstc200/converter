package cn.edu.bupt.util;

import cn.edu.bupt.dao.dbTokenImpl;
import cn.edu.bupt.data.CameraInfo;

/**
 * @Description: TokenUtil
 * @Author: czx
 * @CreateDate: 2019-05-24 14:35
 * @Version: 1.0
 */
public class TokenUtil {

    public static final dbTokenImpl db = new dbTokenImpl();

    public static String getToken(String cameraName){
        String token = null;
        if(db.get(cameraName) == null){ //SQLite里没有token
            try{
                String session = HttpUtil.login();
                String id = HttpUtil.createDevice(cameraName,session);
                token = HttpUtil.findToken(id,session);
            }catch (Exception e){
                e.printStackTrace();
                return null;
            }//存入DB
            if(token==null){
                System.out.println("null token");
                return null;
            }
            db.insert(cameraName,token);
            return token;
        }else{//SQLite里有token，从表中拿token
            token = db.get(cameraName);
            return token;
        }
    }
}
