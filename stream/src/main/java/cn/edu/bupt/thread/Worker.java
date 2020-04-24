package cn.edu.bupt.thread;

import cn.edu.bupt.tasks.Task;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class Worker implements ExecutorService {

//    private final PriorityBlockingQueue<Task> queue;
//    private final LinkedBlockingQueue<Task> queue = new LinkedBlockingQueue<>();
    private final DynamicFreshQueue queue = new DynamicFreshQueue();

    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

    private LinkedBlockingQueue<Runnable> runnables = new LinkedBlockingQueue<>();

    private static final AtomicInteger one = new AtomicInteger(0);
    private static final AtomicInteger two = new AtomicInteger(0);

    private static final List<Integer> ones = new ArrayList<>();
    private static final List<Integer> twos = new ArrayList<>();

    public Worker() {
//        this.queue = new PriorityBlockingQueue<>(11, new Comparator<Task>() {
//            @Override
//            public int compare(Task o1, Task o2){
//                if(o1.getPriority() == o2.getPriority()) {
//                    return (int) (o1.getEndTime() - o2.getEndTime());
//                }
//                if (o1.getEndTime() == o2.getEndTime()) {
//                    return o1.getPriority() - o2.getPriority();
//                }
//                long now = System.currentTimeMillis();
//                if (now >= o1.getEndTime() && now < o2.getEndTime()) {
//                    return -1;
//                }
//                if (now >= o2.getEndTime() && now < o1.getEndTime()) {
//                    return 1;
//                }
//                if (now > o2.getEndTime() && now > o1.getEndTime()) {
//                    return o1.getPriority() - o2.getPriority();
//                }
//                if (now < o2.getEndTime() && now < o1.getEndTime()) {
//                    return o1.getPriority() - o2.getPriority();
//                }
//                return o1.getPriority() - o2.getPriority();
//            }
//        });
        new Thread(new Runnable() {
            @Override
            public void run() {
                while(true) {
                    try {
                        while(!runnables.isEmpty()) {
                            Runnable runnable = runnables.poll();
                            runnable.run();
                        }
                        Task task = queue.take();
                        if (task.getPriority() == 1) {
                            one.getAndIncrement();
                        }
                        if (task.getPriority() == 2) {
                            two.getAndIncrement();
                        }
                        task.run();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }).start();
        final AtomicInteger count = new AtomicInteger(0);
        executorService.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                int o = one.get();
                int t = two.get();

                ones.add(o);
                twos.add(t);
                if (count.getAndIncrement() == 100) {
                    System.out.println("ONES:");
                    for (Integer a : ones) {
                        System.out.println(a);
                    }
                    System.out.println("TWOS:");
                    for (Integer a : twos) {
                        System.out.println(a);
                    }
                    System.exit(0);
                }
            }
        },1,1,TimeUnit.SECONDS);
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
        try {
            boolean result = queue.add((Task) task);
            if (result) {
                runnables.add(new Runnable() {
                    @Override
                    public void run() {
                        queue.refreshToReadyTasks();
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
//        queue.put((Task) task);
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
