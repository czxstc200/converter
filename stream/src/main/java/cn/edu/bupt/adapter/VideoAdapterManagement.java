package cn.edu.bupt.adapter;

import org.apache.commons.lang3.concurrent.BasicThreadFactory;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.*;

public class VideoAdapterManagement<T extends VideoAdapter> {

    public Map<String, T> adapters = new ConcurrentHashMap<>();

    private Map<String, Future<String>> futures = new ConcurrentHashMap<>();

    private ExecutorService executorService = Executors.newCachedThreadPool(new BasicThreadFactory.Builder().namingPattern("Adapter-%d").daemon(false).build());

    public VideoAdapterManagement() {
    }

    public void startAdapter(T adapter) throws Exception {
        if (adapters.containsKey(adapter.getName())) {
            throw new Exception("This cn.edu.bupt.adapter name[" + adapter.getName() + "] exists!");
        }
        adapters.put(adapter.getName(), adapter);
        Future<String> future = executorService.submit(() -> {
            adapter.start();
            return String.valueOf(System.currentTimeMillis());
        });
        futures.put(adapter.getName(), future);
    }

    public void stopAdapter(VideoAdapter adapter) {
        adapter.stop();
        futures.remove(adapter.getName());
        adapters.remove(adapter.getName());
    }

    public T getVideoAdapter(String name) {
        return adapters.get(name);
    }

    public boolean getAdapterStatus(String adapterName) {
        return futures.get(adapterName).isDone();
    }

    public Set<String> getAllStreams() {
        Set<String> sources = new HashSet<>();
        for (Map.Entry<String, T> entry : adapters.entrySet()) {
            sources.add(((RTSPVideoAdapter) entry.getValue()).getRTMPPath());
        }
        return sources;
    }
}
