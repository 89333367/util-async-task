package sunyu.util;

/**
 * 重试逻辑
 *
 * @author SunYu
 */
public class RetryLogic {
    /**
     * 重试次数
     */
    private int retry = 3;
    /**
     * 重试间隔时间，单位：毫秒
     */
    private int waitTime = 1000 * 10;

    /**
     * 重试逻辑，默认重试3次，每次间隔10s
     */
    public RetryLogic() {
    }

    /**
     * 重试次数
     *
     * @param retry 重试次数
     */
    public RetryLogic(int retry) {
        this.retry = retry;
    }

    /**
     * 重试次数
     *
     * @param retry    重试次数
     * @param waitTime 重试间隔时间，单位：毫秒
     */
    public RetryLogic(int retry, int waitTime) {
        this.retry = retry;
        this.waitTime = waitTime;
    }

    public int getRetry() {
        return retry;
    }

    public void setRetry(int retry) {
        this.retry = retry;
    }

    public int getWaitTime() {
        return waitTime;
    }

    public void setWaitTime(int waitTime) {
        this.waitTime = waitTime;
    }
}
