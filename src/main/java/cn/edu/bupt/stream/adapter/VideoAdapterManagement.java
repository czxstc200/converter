package cn.edu.bupt.stream.adapter;

import cn.edu.bupt.util.VideoConverter;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.*;

/**
 * @Description: VideoAdapterManagement
 * @Author: czx
 * @CreateDate: 2018-12-07 15:42
 * @Version: 1.0
 */
@Component
public class VideoAdapterManagement {

    private static Map<String,VideoAdapter> map = new ConcurrentHashMap<>();

    private static Map<String,Future<String>> futures = new ConcurrentHashMap<>();

    private static ExecutorService executorService = new ScheduledThreadPoolExecutor( 6,new BasicThreadFactory.Builder().namingPattern("Adapter-pool-%d").daemon(false).build());

    public VideoAdapterManagement(){

    }

    public static void startAdapter(VideoAdapter adapter) throws Exception{
        if(map.containsKey(adapter.getName())){
            throw new Exception("This adapter name["+adapter.getName()+"] exists!");
        }
        map.put(adapter.getName(),adapter);
        Future<String> future = executorService.submit(new Callable<String>() {
            @Override
            public String call() throws Exception {
                adapter.start();
                return String.valueOf(System.currentTimeMillis());
            }
        });
        futures.put(adapter.getName(),future);
    }

    public static void stopAdapter(VideoAdapter adapter){
        adapter.stop();
        futures.get(adapter.getName()).cancel(false);
        futures.remove(adapter.getName());
        map.remove(adapter.getName());
    }

    public static VideoAdapter getVideoAdapter(String name){
        return map.get(name);
    }

    public static boolean getAdapterStatus(String adapterName){
        return futures.get(adapterName).isDone();
    }

    public static void main(String[] args) {

        RtspVideoAdapter rtspVideoAdapter = new RtspVideoAdapter("rtsp://admin:LITFYL@10.112.239.157:554/h264/ch1/main/av_stream","rtmp://10.112.17.185/oflaDemo/haikang1",true);
        try {
            VideoAdapterManagement.startAdapter(rtspVideoAdapter);
            Thread.sleep(20000);
        }catch (Exception e){

        }
        System.out.println("************************");
        System.out.println("是否完成转录："+VideoAdapterManagement.getAdapterStatus("rtmp://10.112.17.185/oflaDemo/haikang1"));
//        VideoAdapterManagement.stopAdapter(VideoAdapterManagement.getVideoAdapter("rtmp://10.112.17.185/oflaDemo/haikang1"));

    }
}
