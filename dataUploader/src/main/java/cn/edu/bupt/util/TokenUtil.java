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

    public static String getToken(CameraInfo cameraInfo){
        String token = null;
        String serialNumber= cameraInfo.getSerialNumber();
        if(db.get(serialNumber) == null){ //SQLite里没有token
            try{
                String session = HttpUtil.login();
                String id = HttpUtil.createDevice(cameraInfo.getSerialNumber(),session);
                token = HttpUtil.findToken(id,session);
            }catch (Exception e){
                e.printStackTrace();
                return null;
            }//存入DB
            if(token==null){
                System.out.println("null token");
                return null;
            }
            db.insert(serialNumber,token);
            return token;
        }else{//SQLite里有token，从表中拿token
            token = db.get(serialNumber);
            return token;
        }
    }
}
