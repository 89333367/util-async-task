package sunyu.util;

import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.IdUtil;
import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;

import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;

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
        // 创建有界队列线程池（队列容量=并发数）
        config.executor = new ThreadPoolExecutor(config.maxConcurrency, config.maxConcurrency, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<>(config.maxConcurrency * 2), (r, e) -> {
            try {
                e.getQueue().put(r); // 阻塞式入队
            } catch (InterruptedException ignored) {
            }
        });
        log.info("[构建AsyncTaskUtil] 结束");
        this.config = config;
    }

    private static class Config {
        private ExecutorService executor;
        private final Map<String, CompletableFuture<?>> completableFutureMap = new ConcurrentHashMap<>();
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
     * 提交异步任务
     *
     * @param task 需要执行的任务逻辑
     */
    public void submitTask(Runnable task) {
        submitTask(task, null);
    }

    /**
     * 提交异步任务
     *
     * @param task             需要执行的任务逻辑
     * @param exceptionHandler 异常处理回调
     */
    public void submitTask(Runnable task, Consumer<Throwable> exceptionHandler) {
        submitTask(task, exceptionHandler, new RetryLogic(0));
    }

    /**
     * 提交异步任务
     *
     * @param task             需要执行的任务逻辑
     * @param exceptionHandler 异常处理回调
     * @param retryLogic       重试逻辑
     */
    public void submitTask(Runnable task, Consumer<Throwable> exceptionHandler, RetryLogic retryLogic) {
        String taskId = IdUtil.simpleUUID();
        CompletableFuture<Void> future = new CompletableFuture<>();
        config.completableFutureMap.put(taskId, future);
        CompletableFuture.runAsync(() -> {
            int retryCount = 0;
            while (true) {
                try {
                    task.run();
                    break;
                } catch (Exception e) {
                    if (retryLogic == null) {
                        log.warn("[任务执行失败] 等待 10s 后进行无限重试");
                        ThreadUtil.sleep(1000 * 10);
                    } else {
                        if (retryLogic.getRetry() < ++retryCount) {
                            throw new RuntimeException(e);
                        } else {
                            log.warn("[任务执行失败] 等待 {}ms 后进行第 {} 次重试", retryLogic.getWaitTime(), retryCount);
                            ThreadUtil.sleep(retryLogic.getWaitTime());
                        }
                    }
                }
            }
        }, config.executor).exceptionally(throwable -> {
            if (throwable != null) {
                if (exceptionHandler != null) {
                    exceptionHandler.accept(throwable.getCause());
                }
            }
            return null;
        }).whenComplete((unused, throwable) -> {
            config.completableFutureMap.remove(taskId);
        }).whenComplete((unused, throwable) -> {
            future.complete(null);
        });
    }

    /**
     * 等待所有任务完成
     */
    public void awaitAllTasks() {
        CompletableFuture.allOf(config.completableFutureMap.values().toArray(new CompletableFuture[0])).join();
    }

    /**
     * 获得正在执行或等待中的异步任务数量
     *
     * @return 未完成的任务数量
     */
    public int getPendingTaskCount() {
        return config.completableFutureMap.size();
    }

}