package theadpool;

import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

//TODO idle time에 따라 thread 감소
public class MyThreadPool implements Executor {

    private final BlockingQueue<Runnable> queue;

    private final AtomicBoolean shutdown = new AtomicBoolean(false);
    private final Set<Thread> workerThreads;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    /**
     * shut down 시 활용
     */
    private static final Runnable LAST_TASK = () -> {
    };

    private final int maxThreadNums;
    private final AtomicInteger currentWorkerNums;

    public MyThreadPool(int corePoolSize) {
        this(corePoolSize, corePoolSize, Integer.MAX_VALUE);
    }

    public MyThreadPool(int corePoolSize, int maxThreadNums) {
        this(corePoolSize, maxThreadNums, Integer.MAX_VALUE);
    }

    public MyThreadPool(int corePoolSize, int maxWorkerNums, int queueSize) {
        if (corePoolSize > maxWorkerNums) {
            throw new IllegalArgumentException("maxWorkerNums cannot exceed threadNums");
        }
        if (queueSize < 0) {
            throw new IllegalArgumentException("invalid queue size");
        }

        this.queue = new LinkedBlockingQueue<>(queueSize);
        this.maxThreadNums = maxWorkerNums;
        this.workerThreads = new HashSet<>(corePoolSize);
        for (int i = 0; i < corePoolSize; i++) {
            workerThreads.add(newThread());
        }
        this.currentWorkerNums = new AtomicInteger(workerThreads.size());
    }

    private Thread newThread() {
        return new Thread(() -> {
            while (true) {
                Runnable task = null;
                try {
                    task = queue.take();
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
                }
            }
        });
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    @Override
    public void execute(@NotNull Runnable command) {
        if (initialized.compareAndSet(false, true)) {
            for (Thread t : workerThreads) {
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
            if (current >= maxThreadNums) {
                throw new RejectedExecutionException("Queue is full and thread pool is at maximum size");
            }

            if (currentWorkerNums.compareAndSet(current, current + 1)) {
                try {
                    Thread thread = newThread();
                    thread.start();
                    workerThreads.add(thread);
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

        for (Thread t : workerThreads) {
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
}
