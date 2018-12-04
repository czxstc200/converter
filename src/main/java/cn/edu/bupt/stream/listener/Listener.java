package cn.edu.bupt.stream.listener;

import cn.edu.bupt.stream.event.Event;

import java.util.EventListener;

/**
 * @Description: Listener
 * @Author: czx
 * @CreateDate: 2018-12-02 15:55
 * @Version: 1.0
 */
public interface Listener extends EventListener {

    /**
     * @Description 当事件（拉流）发生时调用，用于完成功能
     * @author CZX
     * @date 2018/12/4 9:32
     * @param [event]
     * @return void
     */
    void fireAfterEventInvoked(Event event);

    /**
     * @Description 获取监听器的名字
     * @author CZX
     * @date 2018/12/4 9:33
     * @param []
     * @return java.lang.String
     */
    String getName();

    /**
     * @Description 启动监听器
     * @author CZX
     * @date 2018/12/4 9:33
     * @param []
     * @return void
     */
    void start();

    /**
     * @Description 关闭监听器
     * @author CZX
     * @date 2018/12/4 9:33
     * @param []
     * @return void
     */
    void close();
}
