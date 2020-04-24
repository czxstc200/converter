package cn.edu.bupt.thread;

import cn.edu.bupt.tasks.Task;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class DynamicFreshQueue {

    private final List<Queue<Task>> queues = new ArrayList<>();

    private final Queue<Task> readyTasks = new LinkedBlockingQueue<>();

    AtomicBoolean refreshed = new AtomicBoolean(false);

    private final ReentrantLock lock;
    private final Condition notEmpty;

    public DynamicFreshQueue() {
        queues.add(new LinkedBlockingQueue<>());
        queues.add(new LinkedBlockingQueue<>());
        queues.add(new LinkedBlockingQueue<>());
        lock = new ReentrantLock();
        notEmpty = lock.newCondition();
    }

    public boolean isEmpty() {
        boolean result = true;
        for(Queue<Task> tasks : queues) {
            result &= tasks.isEmpty();
        }
        return result;
    }

    public boolean add(Task task) throws InterruptedException {
        lock.lockInterruptibly();
        try {
            Queue<Task> queue = queues.get(task.getPriority() - 1);
            queue.add(task);
            boolean result = false;
            Task earliestTask = queue.peek();
            if (readyTasks.isEmpty() && earliestTask!=null && !refreshed.get()) {
                result = System.currentTimeMillis() <= earliestTask.getEndTime();
                refreshed.set(true);
            }
            notEmpty.signal();
            return result;
        } finally {
            lock.unlock();
        }
    }

    public void refreshToReadyTasks() {
        System.out.println("refresh before:" + readyTasks.size());
        for (Queue<Task> queue : queues) {
            while(true) {
                Task task = queue.peek();
                if (queue.isEmpty() || task == null) {
                    break;
                }
                if (System.currentTimeMillis() <= task.getEndTime()) {
                    readyTasks.add(queue.poll());
                } else {
                    break;
                }
            }
        }
        refreshed.set(false);
        System.out.println("refresh after:" + readyTasks.size());
    }

    public Task take() throws InterruptedException {
        if (!readyTasks.isEmpty()) {
            return readyTasks.poll();
        }
        lock.lockInterruptibly();
        try {
            while (isEmpty()) {
                notEmpty.await();
            }
            return pollOne();
        } finally {
            lock.unlock();
        }
    }

    private Task pollOne() {
        for (Queue<Task> tasks : queues) {
            if (!tasks.isEmpty()) {
                return tasks.poll();
            }
        }
        return null;
    }
}
