package cn.edu.bupt.adapter;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

public class VideoAdapterManagement<T extends VideoAdapter> {

    public Map<String, T> map = new ConcurrentHashMap<>();

    private Map<String, Future<String>> futures = new ConcurrentHashMap<>();

    private ExecutorService executorService = Executors.newCachedThreadPool(new BasicThreadFactory.Builder().namingPattern("Adapter-%d").daemon(false).build());

    public VideoAdapterManagement() {
    }

    public void startAdapter(T adapter) throws Exception {
        if (map.containsKey(adapter.getName())) {
            throw new Exception("This cn.edu.bupt.adapter name[" + adapter.getName() + "] exists!");
        }
        map.put(adapter.getName(), adapter);
        Future<String> future = executorService.submit(() -> {
            adapter.start();
            return String.valueOf(System.currentTimeMillis());
        });
        futures.put(adapter.getName(), future);
    }

    public void stopAdapter(T adapter) {
        adapter.stop();
        futures.remove(adapter.getName());
        map.remove(adapter.getName());
    }

    public T getVideoAdapter(String name) {
        return map.get(name);
    }

    public boolean getAdapterStatus(String adapterName) {
        return futures.get(adapterName).isDone();
    }

    public Set<String> getAllStreams() {
        Set<String> sources = new HashSet<>();
        for (Map.Entry<String, T> entry : map.entrySet()) {
            sources.add(((RTSPVideoAdapter) entry.getValue()).getRTMPPath());
        }
        return sources;
    }
}
