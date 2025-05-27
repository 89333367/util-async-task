# util-async-task
异步执行任务工具类，具有并发执行和并发大小限制功能


# 引入依赖
```xml
<dependency>
    <groupId>sunyu.util</groupId>
    <artifactId>util-async-task</artifactId>
    <!-- {util.version}_{jdk.version}_{architecture.version} -->
    <version>1.0_jdk8_x64</version>
</dependency>
```

# 接口
```java
/**
 * 提交任务
 *
 * @param task
 */
public void submitTask(Runnable task)

/**
 * 提交异步任务
 *
 * @param task             需要执行的任务逻辑
 * @param exceptionHandler 异常处理回调
 */
public void submitTask(Runnable task, Consumer<Throwable> exceptionHandler)

/**
 * 提交异步任务
 *
 * @param task             需要执行的任务逻辑
 * @param exceptionHandler 异常处理回调
 * @param retryLogic       重试逻辑
 */
public void submitTask(Runnable task, Consumer<Throwable> exceptionHandler, RetryLogic retryLogic)
```

# 测试代码
```java
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import org.junit.jupiter.api.Test;
import sunyu.util.AsyncTaskUtil;
import sunyu.util.RetryLogic;

import java.util.function.Consumer;

public class TestUtil {
    private final Log log = LogFactory.get();

    @Test
    void t001() {
        AsyncTaskUtil asyncTaskUtil = AsyncTaskUtil.builder().setMaxConcurrency(10).build();//构建实例
        asyncTaskUtil.submitTask(new Runnable() {
            @Override
            public void run() {
                ThreadUtil.sleep(3000);
                log.info("处理完毕");
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) {
                log.error(throwable.getMessage());
            }
        });
        asyncTaskUtil.awaitAllTasks();//等待所有异步任务完成
        asyncTaskUtil.close();//回收资源
    }

    @Test
    void t002() {
        AsyncTaskUtil asyncTaskUtil = AsyncTaskUtil.builder().setMaxConcurrency(10).build();//构建实例
        asyncTaskUtil.submitTask(new Runnable() {
            @Override
            public void run() {
                ThreadUtil.sleep(3000);
                int i = 1 / 0;//模拟报错
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) {
                log.error(throwable.getMessage());
                //log.error(throwable);
            }
        });
        asyncTaskUtil.awaitAllTasks();//等待所有异步任务完成
        asyncTaskUtil.close();//回收资源
    }

    @Test
    void t003() {
        AsyncTaskUtil asyncTaskUtil = AsyncTaskUtil.builder().setMaxConcurrency(10).build();//构建实例
        for (int i = 0; i < 100; i++) {
            int finalI = i;
            asyncTaskUtil.submitTask(new Runnable() {
                @Override
                public void run() {
                    log.info("{} 处理开始 当前任务数量 {}", finalI, asyncTaskUtil.getPendingTaskCount());
                    ThreadUtil.sleep(RandomUtil.randomInt(1000, 5000));
                    log.info("{} 处理结束 当前任务数量 {}", finalI, asyncTaskUtil.getPendingTaskCount());
                }
            }, new Consumer<Throwable>() {
                @Override
                public void accept(Throwable throwable) {
                    log.error(throwable.getMessage());
                }
            });
        }
        asyncTaskUtil.awaitAllTasks();//等待所有异步任务完成
        log.info("当前任务数量 {}", asyncTaskUtil.getPendingTaskCount());
        asyncTaskUtil.close();//回收资源
    }


    @Test
    void t004() {
        AsyncTaskUtil asyncTaskUtil = AsyncTaskUtil.builder().setMaxConcurrency(10).build();//构建实例
        asyncTaskUtil.submitTask(new Runnable() {
            @Override
            public void run() {
                log.info("执行任务，当前正在执行任务数量 {}", asyncTaskUtil.getPendingTaskCount());
                ThreadUtil.sleep(3000);
                int i = 1 / 0;//模拟报错
            }
        }, new Consumer<Throwable>() {
            @Override
            public void accept(Throwable throwable) {
                log.error(throwable.getMessage());
                //log.error(throwable);
            }
        }, new RetryLogic(1, 2000));
        asyncTaskUtil.awaitAllTasks();//等待所有异步任务完成
        asyncTaskUtil.close();//回收资源
    }


}
```


