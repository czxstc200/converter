package cn.edu.bupt.tasks;

public interface Task extends Runnable {

    int getPriority();

    long getEndTime();


}
