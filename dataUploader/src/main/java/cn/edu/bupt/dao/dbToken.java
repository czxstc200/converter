package cn.edu.bupt.dao;

public interface dbToken {
    void insert(String serialNumber, String token);
    boolean delete(String serialNumber);
    boolean update(String serialNumber, String token);
    String get(String serialNumber);
}
