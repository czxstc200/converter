package cn.edu.bupt.adapter;

import cn.edu.bupt.listener.Listener;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Slf4j
@Data
public abstract class VideoAdapter {

    protected final List<Listener> listeners = new ArrayList<>();

    protected final Set<Class<? extends Listener>> listenerSet = new HashSet<>();

    protected String name;

    protected VideoAdapterManagement<? extends VideoAdapter> videoAdapterManagement;

    VideoAdapter(String name, VideoAdapterManagement<? extends VideoAdapter> videoAdapterManagement) {
        this.name = name;
        this.videoAdapterManagement = videoAdapterManagement;
    }

    public boolean addListener(Listener listener) {
        log.info("Add listener[{}] to VideoAdapter[{}]", listener.getName(), name);
        if (listenerSet.add(listener.getClass())) {
            return listeners.add(listener);
        } else {
            log.info("Add listener failed, listener class:[{}]", listener.getClass().getName());
            return false;
        }
    }

    public boolean removeListener(Listener listener) {
        log.info("Remove listener[{}] from VideoAdapter[{}]", listener.getName(), name);
        listenerSet.remove(listener.getClass());
        return listeners.remove(listener);
    }

    public boolean removeListener(Class listenerClass) {
        Listener removedListener = null;
        for (Listener listener : listeners) {
            if (listener.getClass() == listenerClass) {
                removedListener = listener;
                break;
            }
        }
        if (removedListener == null) {
            return false;
        }
        listenerSet.remove(listenerClass);
        listeners.remove(removedListener);
        try {
            removedListener.close();
            return true;
        } catch (Exception e) {
            log.error("Listener failed to close, listenerName:[{}], e:", removedListener.getName(), e);
            return false;
        }
    }

    public abstract void start() throws Exception;

    public abstract void stop();
}
