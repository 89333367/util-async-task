package sunyu.util;

import cn.hutool.core.exceptions.ExceptionUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 异步任务工具类
 *
 * @author SunYu
 */
public class AsyncTaskUtil implements AutoCloseable {
    private final Log log = LogFactory.get();
    private final Config config;

    public static Builder builder() {
        return new Builder();
    }

    private AsyncTaskUtil(Config config) {
        log.info("[构建AsyncTaskUtil] 开始");
        config.executor = Executors.newFixedThreadPool(config.maxConcurrency);
        config.countLatchUtil = CountLatchUtil.builder().setMaxCount(config.maxConcurrency).build();
        log.info("[构建AsyncTaskUtil] 结束");
        this.config = config;
    }

    private static class Config {
        private ExecutorService executor;
        private CountLatchUtil countLatchUtil;

        private Integer maxConcurrency = 10;
    }

    public static class Builder {
        private final Config config = new Config();

        /**
         * 构建实例
         *
         * @return 工具实例
         */
        public AsyncTaskUtil build() {
            return new AsyncTaskUtil(config);
        }

        /**
         * 设置最大并发数
         *
         * @param maxConcurrency 最大并发数，默认10
         * @return 工具实例
         */
        public Builder setMaxConcurrency(int maxConcurrency) {
            config.maxConcurrency = maxConcurrency;
            return this;
        }
    }

    /**
     * 回收资源
     */
    @Override
    public void close() {
        log.info("[销毁AsyncTaskUtil] 开始");
        awaitAllTasks();
        config.executor.shutdown();
        log.info("[销毁AsyncTaskUtil] 结束");
    }

    /**
     * 获取当前任务数
     *
     * @return 当前任务数量
     */
    public int getCount() {
        return config.countLatchUtil.getCount();
    }

    /**
     * 提交任务
     *
     * @param task
     */
    public void submitTask(Runnable task) {
        submitTask(task, null, null);
    }

    /**
     * 提交任务
     *
     * @param task        任务
     * @param maxAttempts 最大重试次数，不需要重试请填写0，如果填写null则无限重试
     */
    public void submitTask(Runnable task, Integer maxAttempts) {
        submitTask(task, maxAttempts, null);
    }

    /**
     * 提交任务
     *
     * @param task        任务
     * @param maxAttempts 最大重试次数，不需要重试请填写0，如果填写null则无限重试
     * @param sleepMillis 每次重试间隔毫秒数，如果为null则每次重试间隔1000毫秒
     */
    public void submitTask(Runnable task, Integer maxAttempts, Integer sleepMillis) {
        config.countLatchUtil.countUp();//任务开始前，计数器加一
        AtomicReference<String> err = new AtomicReference<>();
        config.executor.submit(() -> {
            int attempts = 0;
            do {
                try {
                    task.run();
                    break;
                } catch (Exception e) {
                    err.set(ExceptionUtil.stacktraceToString(e));
                    attempts++;
                    log.warn("[重试] 第 {} 次", attempts);
                    if (sleepMillis != null) {
                        ThreadUtil.sleep(sleepMillis);
                    } else {
                        ThreadUtil.sleep(1000);
                    }
                }
            } while (maxAttempts == null || attempts < maxAttempts);
            config.countLatchUtil.countDown();//任务完成，计数器减一
            if (err.get() != null) {
                log.error("[任务执行失败] {}", err.get());
            }
        });
    }

    /**
     * 等待所有任务完成
     */
    public void awaitAllTasks() {
        config.countLatchUtil.await();
    }

}