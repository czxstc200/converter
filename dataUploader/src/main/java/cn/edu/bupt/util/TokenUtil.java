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
        String token;
        if(db.get(cameraName) == null){ //SQLite里没有token
            try {
                String session = HttpUtil.login();
                String id = HttpUtil.createDevice(cameraName, session);
                if (id == null || id.equals("")) {
                    id = HttpUtil.findDeviceId(cameraName,session);
                    System.out.println("new id : "+id);
                }
                token = HttpUtil.findToken(id, session);
                System.out.println(token);
                db.insert(cameraName, token);
            }catch (Exception e){
                e.printStackTrace();
                return null;
            }//存入DB
            return token;
        }else{//SQLite里有token，从表中拿token
            token = db.get(cameraName);
            return token;
        }
    }
}
