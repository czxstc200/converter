package cn.edu.bupt.dao;

/**
 * Created by zyf on 2018/6/13.
 */
public interface dbToken {

    public void insert(String serialNumber, String token);
    public boolean delete(String serialNumber);
    public boolean update(String serialNumber, String token);
    public String get(String serialNumber);
}
