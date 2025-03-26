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
 * 提交任务
 *
 * @param task        任务
 * @param maxAttempts 最大重试次数，不需要重试请填写0，如果填写null则无限重试
 */
public void submitTask(Runnable task, Integer maxAttempts)

/**
 * 提交任务
 *
 * @param task        任务
 * @param maxAttempts 最大重试次数，不需要重试请填写0，如果填写null则无限重试
 * @param sleepMillis 每次重试间隔毫秒数，如果为null则每次重试间隔1000毫秒
 */
public void submitTask(Runnable task, Integer maxAttempts, Integer sleepMillis)
```

# 测试代码
```java
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;
import org.junit.jupiter.api.Test;
import sunyu.util.AsyncTaskUtil;

public class TestUtil {
    private final Log log = LogFactory.get();

    @Test
    void t001() {
        AsyncTaskUtil asyncTaskUtil = AsyncTaskUtil.builder().setMaxConcurrency(10).build();//构建实例
        for (int i = 0; i < 30; i++) {
            int finalI = i;
            asyncTaskUtil.submitTask(() -> {//提交任务
                log.info("[开始] 任务{} {} 当前正在执行任务数量 {}", finalI, Thread.currentThread().getName(), asyncTaskUtil.getCount());
                ThreadUtil.sleep(RandomUtil.randomInt(500, 5000));//模拟任务执行时间
                log.info("[结束] 任务{} {} 当前正在执行任务数量 {}", finalI, Thread.currentThread().getName(), asyncTaskUtil.getCount());
            }, 1);
        }
        asyncTaskUtil.awaitAllTasks();//等待所有异步任务完成
        log.info("当前正在执行任务数量 {}", asyncTaskUtil.getCount());
        asyncTaskUtil.close();//回收资源
    }

    @Test
    void t002() {
        AsyncTaskUtil asyncTaskUtil = AsyncTaskUtil.builder().setMaxConcurrency(10).build();//构建实例
        for (int k = 1; k <= 3; k++) {
            log.info("[开始] 第 {} 批任务", k);
            for (int i = 0; i < 30; i++) {
                int finalI = i;
                asyncTaskUtil.submitTask(() -> {//提交任务
                    log.info("[开始] 任务{} {} 当前正在执行任务数量 {}", finalI, Thread.currentThread().getName(), asyncTaskUtil.getCount());
                    ThreadUtil.sleep(RandomUtil.randomInt(500, 5000));//模拟任务执行时间
                    log.info("[结束] 任务{} {} 当前正在执行任务数量 {}", finalI, Thread.currentThread().getName(), asyncTaskUtil.getCount());
                }, 1);
            }
            asyncTaskUtil.awaitAllTasks();//等待所有异步任务完成
            log.info("[结束] 第 {} 批任务", k);
        }
        log.info("当前正在执行任务数量 {}", asyncTaskUtil.getCount());
        asyncTaskUtil.close();//回收资源
    }

}
```


