//package cn.edu.bupt.test;
//
//import cn.edu.bupt.client.NettyClient;
//import cn.edu.bupt.client.NettyClientFactory;
//import cn.edu.bupt.client.NettyClientPool;
//
//import java.util.Random;
//import java.util.concurrent.ExecutorService;
//import java.util.concurrent.Executors;
//import java.util.concurrent.ScheduledExecutorService;
//import java.util.concurrent.TimeUnit;
//import java.util.concurrent.atomic.AtomicInteger;
//
///**
// * @Description: Test
// * @Author: czx
// * @CreateDate: 2020-04-22 20:09
// * @Version: 1.0
// */
//public class Test {
//
//    private static NettyClientPool clientPool = new NettyClientPool(new NettyClientFactory("",1));
//
//    public static void main(String[] args) throws Exception {
//        AtomicInteger atomicInteger = new AtomicInteger(0);
//        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
//        executorService.scheduleAtFixedRate(new Runnable() {
//            @Override
//            public void run() {
//                long vmFree = 0;
//                long vmUse = 0;
//                long vmTotal = 0;
//                int byteToMb = 1024 * 1024;
//                Runtime rt = Runtime.getRuntime();
//                vmTotal = rt.totalMemory() / byteToMb;
//                vmFree = rt.freeMemory() / byteToMb;
//                vmUse = vmTotal - vmFree;
//                System.out.println(vmUse);
//                atomicInteger.getAndIncrement();
//                if (atomicInteger.get()==125) {
//                    System.exit(0);
//                }
//            }
//        }, 1, 1, TimeUnit.SECONDS);
//        clientPool.setMaxTotal(-1);
//        ExecutorService executorService1 = Executors.newCachedThreadPool();
//        while(true) {
//            executorService1.submit(new Runnable() {
//                @Override
//                public void run() {
//                    try {
////                        NettyClient client = new NettyClient("",1,null);
//                        NettyClient client = clientPool.borrowObject();
//                        Random random = new Random();
//                        long num = (long)(Math.sqrt(500)*random.nextGaussian())+500;
//                        Thread.sleep(Math.abs(num));
//                        clientPool.returnObject(client);
//                    } catch (Exception e) {
//                        e.printStackTrace();
//                    }
//                }
//            });
//            Random random = new Random();
//            long num1 = (long)(Math.sqrt(250)*random.nextGaussian())+250;
//            Thread.sleep(Math.abs(num1));
//        }
//
//    }
//}
