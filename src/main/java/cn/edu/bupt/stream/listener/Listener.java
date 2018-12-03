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

    public void fireAfterEventInvoked(Event event);

    public String getName();

    public void start();

    public void close();
}
