package theadpool;

import org.jetbrains.annotations.NotNull;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class MyThreadPool implements Executor {

    private final BlockingQueue<Runnable> queue;

    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final Map<Thread, Boolean> workerThreads;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * shut down 시 활용
     */
    private static final Runnable LAST_TASK = () -> {
    };

    private final int corePoolSize;
    private final int maxPoolSize;
    private final AtomicInteger currentWorkerNums;

    private final long keepAliveTimeMillis;

    public MyThreadPool(int corePoolSize, int maxPoolSize, int queueSize, long keepAliveTime, TimeUnit unit) {
        if (corePoolSize > maxPoolSize) {
            throw new IllegalArgumentException("maxPoolSize cannot exceed threadNums");
        }

        if (queueSize < 0) {
            throw new IllegalArgumentException("invalid queue size");
        }

        if (keepAliveTime < 0) {
            throw new IllegalArgumentException("invalid queue size");
        }

        this.corePoolSize = corePoolSize;
        this.keepAliveTimeMillis = unit.toMillis(keepAliveTime);
        this.queue = new LinkedBlockingQueue<>(queueSize);
        this.maxPoolSize = maxPoolSize;
        this.workerThreads = new ConcurrentHashMap<>(corePoolSize);

        for (int i = 0; i < corePoolSize; i++) {
            workerThreads.put(newThread(), Boolean.TRUE);
        }
        this.currentWorkerNums = new AtomicInteger(workerThreads.size());
    }

    private Thread newThread() {
        return new Thread(() -> {
            while (true) {
                Runnable task = null;
                try {
                    task = queue.poll(keepAliveTimeMillis, TimeUnit.MILLISECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }

                if (task != null) {
                    if (task == LAST_TASK) {
                        return;
                    }

                    try {
                        task.run();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    int current = currentWorkerNums.get();
                    if (current > corePoolSize) {
                        if (currentWorkerNums.compareAndSet(current, current - 1)) {
                            workerThreads.remove(Thread.currentThread());
                            return;
                        }
                    }
                }
            }
        });
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void execute(@NotNull Runnable command) {
        if (initialized.compareAndSet(false, true)) {
            for (Thread t : workerThreads.keySet()) {
                t.start();
            }
        }

        if (shutdown.get()) {
            throw new RejectedExecutionException();
        }

        submitTask(command);

        // 만일 이미 shutdown 되어 있다면 제거
        if (shutdown.get()) {
            queue.remove(command);
            throw new RejectedExecutionException();
        }
    }

    /**
     * 큐가 가득 찬 경우 maxThreadNums 까지 스레드를 증가시키면서 큐잉을 시도한다
     */
    private void submitTask(Runnable command) {
        if (queue.offer(command)) {
            return;
        }

        while (!queue.offer(command)) {
            int current = currentWorkerNums.get();
            if (current >= maxPoolSize) {
                throw new RejectedExecutionException("Queue is full and thread pool is at maximum size");
            }

            if (currentWorkerNums.compareAndSet(current, current + 1)) {
                try {
                    Thread thread = newThread();
                    thread.start();
                    workerThreads.put(thread, Boolean.TRUE);
                } catch (Exception e) {
                    currentWorkerNums.decrementAndGet();
                    throw new RejectedExecutionException(e);
                }
            }
        }
    }


    public void shutdown() {
        if (shutdown.compareAndSet(false, true)) {
            for (int i = 0; i < workerThreads.size(); i++) {
                queue.add(LAST_TASK);
            }
        }

        for (Thread t : workerThreads.keySet()) {
            while (t.isAlive()) {
                try {
                    t.join();
                } catch (InterruptedException ignored) {
                }
            }
        }
    }


    public int getWorkerSize() {
        return currentWorkerNums.get();
    }


    public static class Builder {
        public Builder() {
        }

        private int corePoolSize;
        private int maxPoolSize = Integer.MAX_VALUE;
        private int queueSize = Integer.MAX_VALUE;
        private Duration duration;

        public Builder corePoolSize(int corePoolSize) {
            this.corePoolSize = corePoolSize;
            return this;
        }

        public Builder maxPoolSize(int maxPoolSize) {
            this.maxPoolSize = maxPoolSize;
            return this;
        }

        public Builder queueSize(int maxQueueSize) {
            this.queueSize = maxQueueSize;
            return this;
        }

        public Builder keepAliveDuration(Duration duration) {
            this.duration = duration;
            return this;
        }

        public MyThreadPool build() {
            return new MyThreadPool(corePoolSize, maxPoolSize, queueSize, duration.get(ChronoUnit.SECONDS) * 1000, TimeUnit.MILLISECONDS);
        }
    }


    public static Builder builder() {
        return new Builder();
    }
}
