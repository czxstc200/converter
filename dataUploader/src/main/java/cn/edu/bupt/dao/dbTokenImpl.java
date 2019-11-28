package cn.edu.bupt.dao;

public class dbTokenImpl implements dbToken {

    //添加数据
    public void insert(String serialNumber,String token){
        DAO.insert(serialNumber, token);
    }

    //删除数据
    public boolean delete(String serialNumber){
        DAO.delete(serialNumber);
        return true;
    }

    //更新数据
    public boolean update(String serialNumber,String token) {
        DAO.update(serialNumber,token);
        return true;
    }

    //查询数据
    public String get(String serialNumber) {
        return DAO.getAll(serialNumber);
    }
}
