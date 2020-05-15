package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.product.service.TestService;
import io.swagger.annotations.Api;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.*;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * @author Liyanbao
 * @create 2020-04-24 0:37
 */
@Api(tags = "测试接口")
@RestController
@RequestMapping("admin/product/test")
public class TestController {

    @Autowired
    private TestService testService;

    @GetMapping("testLock")
    public Result testLock() {
        testService.testLock();
        return Result.ok();
    }

    @GetMapping("read")
    public Result read() {
        String msg = testService.readLock();
        return Result.ok(msg);
    }

    @GetMapping("write")
    public Result write() {
        String msg = testService.writeLock();
        return Result.ok(msg);
    }

    public static void main(String[] args) throws ExecutionException, InterruptedException {

        //以下demo:并行化
        //创建一个线程池
        ThreadPoolExecutor poolExecutor = new ThreadPoolExecutor(50, 500, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(1000));
        //线程池A：执行的结果：hello
        CompletableFuture<String> futureA = CompletableFuture.supplyAsync(() -> "hello");
        //线程池B
        CompletableFuture<Void> futureB = futureA.thenAcceptAsync((s) -> {
            //先让线程池睡一会
            delaySec(3);
            //打印数据
            printCurrTime(s + "第一个线程");
        }, poolExecutor);
        //线程池C
        CompletableFuture<Void> futureC = futureA.thenAcceptAsync((s) -> {
            //先让线程池睡一会
            delaySec(1);
            //打印数据
            printCurrTime(s + "第二个线程");
        }, poolExecutor);
        //线程B和线程C都要依赖线程池A的结果
        //交替改变两个线程睡眠时间，看看结果，谁睡眠的时间少先执行谁,实现了并行的关系
    }

    private static void printCurrTime(String s) {
        System.out.println(s);
    }

    private static void delaySec(int i) {
        try {
            Thread.sleep(i*1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

    }
//以下代码demo:串行化
        //支持返回值
//        CompletableFuture<Integer> future = CompletableFuture.supplyAsync(new Supplier<Integer>() {
//
//            @Override
//            public Integer get() {
//                System.out.println(Thread.currentThread().getName() + "---->线程名");
//                //如果有异常,自定义的异常
//                //int i = 1 / 0;
//                //返回值
//                return 1024;
//            }
//        }).thenApply(new Function<Integer, Integer>() {
//            @Override
//            public Integer apply(Integer o) {
//                System.out.println("thenApply获取上一个任务返回的结果，并返回当前任务的返回值-->" + o);
//                //返回的数据
//                return o*2;
//            }
//        }).whenComplete(new BiConsumer<Object, Throwable>() {
//            //o：获取上一个的结果
//            //throwable:表示有没有异常
//            @Override
//            public void accept(Object o, Throwable throwable) {
//                System.out.println("o:="+o + ":=====" + o.toString());
//                System.out.println("throwable:="+throwable + ":-----");
//            }
//        }).exceptionally(new Function<Throwable, Integer>() {
//            @Override
//            public Integer apply(Throwable throwable) {
//                System.out.println("throwable=" + throwable + ":======>异常");
//                return 6666;
//            }
//        });
//        //获取结果
//        System.out.println(future.get());
//    }

}
