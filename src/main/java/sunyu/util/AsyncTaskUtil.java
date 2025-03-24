package sunyu.util;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
     * @param task        任务
     * @param maxAttempts 最大重试次数，不需要重试请填写0
     */
    public void submitTask(Runnable task, int maxAttempts) {
        config.countLatchUtil.countUp();//任务开始前，计数器加一
        config.executor.submit(() -> {
            int attempts = 0;
            do {
                try {
                    task.run();
                    break;
                } catch (Exception e) {
                    attempts++;
                    log.warn("[重试] 第 {} 次", attempts);
                }
            } while (attempts < maxAttempts);
            config.countLatchUtil.countDown();//任务完成，计数器减一
        });
    }

    /**
     * 等待所有任务完成
     */
    public void awaitAllTasks() {
        config.countLatchUtil.await();
    }

}