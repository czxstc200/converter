package cn.edu.bupt.thread;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Workers {

    public static List<ExecutorService> getWorkers(int num) {
        List<ExecutorService> executorServices = new ArrayList<>();
        for (int i = 0;i<num; i++) {
            executorServices.add(Executors.newSingleThreadExecutor());
        }
        return executorServices;
    }
}
