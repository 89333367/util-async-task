package sunyu.util;

import cn.hutool.log.Log;
import cn.hutool.log.LogFactory;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 计数器工具
 * <p>
 * 可设置最大计数来阻塞线程
 *
 * @author SunYu
 */
public class CountLatchUtil implements AutoCloseable {
    private final Log log = LogFactory.get();
    private final Config config;

    public static Builder builder() {
        return new Builder();
    }

    private CountLatchUtil(Config config) {
        this.config = config;
    }

    private static class Config {
        private final Lock lock = new ReentrantLock();
        private final Condition zeroCondition = lock.newCondition();
        private final Condition maxCondition = lock.newCondition();
        private int count = 0;//内部计数器

        private Integer maxCount;
    }

    public static class Builder {
        private final Config config = new Config();

        /**
         * 构建实例
         *
         * @return 工具实例
         */
        public CountLatchUtil build() {
            return new CountLatchUtil(config);
        }

        /**
         * 设置最大计数（如果设置了maxCount会阻塞直到有可用空间）
         *
         * @param maxCount 最大数量
         * @return 工具实例
         */
        public Builder setMaxCount(int maxCount) {
            config.maxCount = maxCount;
            return this;
        }
    }

    /**
     * 回收资源
     */
    @Override
    public void close() {
        if (getCount() > 0) {
            log.warn("count 数量未归零 还剩 {} 个", getCount());
        }
    }

    /**
     * 增加计数（如果设置了maxCount会阻塞直到有可用空间）
     */
    public void countUp() {
        config.lock.lock();
        try {
            if (config.maxCount != null) {
                while (config.count >= config.maxCount) {
                    try {
                        config.maxCondition.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("在等待最大计数时被中断", e);
                    }
                }
            }
            config.count++;
            if (config.count == 1) {
                config.zeroCondition.signalAll();
            }
        } finally {
            config.lock.unlock();
        }
    }

    /**
     * 减少计数（如果到达0会唤醒所有等待线程）
     */
    public void countDown() {
        config.lock.lock();
        try {
            if (config.count <= 0) {
                return;
            }
            config.count--;
            if (config.maxCount != null && config.count < config.maxCount) {
                config.maxCondition.signalAll();
            }
            if (config.count == 0) {
                config.zeroCondition.signalAll();
            }
        } finally {
            config.lock.unlock();
        }
    }

    /**
     * 阻塞直到计数器归零
     */
    public void await() {
        config.lock.lock();
        try {
            while (config.count != 0) {
                try {
                    config.zeroCondition.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("在等待归零计数时被中断", e);
                }
            }
        } finally {
            config.lock.unlock();
        }
    }

    /**
     * 获取当前计数值
     *
     * @return 当前计数值
     */
    public int getCount() {
        config.lock.lock();
        try {
            return config.count;
        } finally {
            config.lock.unlock();
        }
    }

}