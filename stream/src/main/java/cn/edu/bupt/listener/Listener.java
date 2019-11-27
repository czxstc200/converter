package cn.edu.bupt.listener;

import cn.edu.bupt.event.Event;

import java.util.EventListener;

public interface Listener extends EventListener {

    void fireAfterEventInvoked(Event event) throws Exception;

    String getName();

    void start();

    void close();
}
