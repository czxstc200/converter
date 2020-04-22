package cn.edu.bupt.thread;

import cn.edu.bupt.tasks.Task;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;

public class Worker implements ExecutorService {

    private final PriorityBlockingQueue<Task> queue;

    public Worker() {
        this.queue = new PriorityBlockingQueue<>(100, new Comparator<Task>() {
            @Override
            public int compare(Task o1, Task o2){
                if(o1.getPriority() == o2.getPriority()) {
                    return (int) (o1.getEndTime() - o2.getEndTime());
                }
                if (o1.getEndTime() == o2.getEndTime()) {
                    return o1.getPriority() - o2.getPriority();
                }
                long now = System.currentTimeMillis();
                if (now >= o1.getEndTime() && now < o2.getEndTime()) {
                    return -1;
                }
                if (now >= o2.getEndTime() && now < o1.getEndTime()) {
                    return 1;
                }
                if (now > o2.getEndTime() && now > o1.getEndTime()) {
                    return o1.getPriority() - o2.getPriority();
                }
                if (now < o2.getEndTime() && now < o1.getEndTime()) {
                    return o1.getPriority() - o2.getPriority();
                }
                return o1.getPriority() - o2.getPriority();
            }
        });
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {
                        Task task = queue.take();
                        task.run();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
    }

    @Override
    public void shutdown() {

    }

    @Override
    public List<Runnable> shutdownNow() {
        return null;
    }

    @Override
    public boolean isShutdown() {
        return false;
    }

    @Override
    public boolean isTerminated() {
        return false;
    }

    @Override
    public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
        return false;
    }

    @Override
    public <T> Future<T> submit(Callable<T> task) {
        return null;
    }

    @Override
    public <T> Future<T> submit(Runnable task, T result) {
        return null;
    }

    @Override
    public Future<?> submit(Runnable task) {
        queue.put((Task) task);
        return null;
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return null;
    }

    @Override
    public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException {
        return null;
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return null;
    }

    @Override
    public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }

    @Override
    public void execute(Runnable command) {

    }
}
