package theadpool;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * simple thread pool
 * 1. initialized with thread pool size
 * 2. shutdown
 * 3. execute task
 */
public class MyThreadPool implements Executor {

    private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    private volatile boolean shutdown;
    private final Thread[] threads;
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private static final Runnable LAST_TASK = () -> {
    };

    public MyThreadPool(int threadNums) {
        this.threads = new Thread[threadNums];
        for (int i = 0; i < threadNums; i++) {
            threads[i] = new Thread(this::run);
        }
    }

    private void run() {
        while (!shutdown || !queue.isEmpty()) {
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
    }

    @Override
    public void execute(@NotNull Runnable command) {
        if (initialized.compareAndSet(false, true)) {
            for (Thread t : threads) {
                t.start();
            }
        }
        if (shutdown) {
            throw new RejectedExecutionException();
        }

        queue.add(command);
    }

    // 더 이상 받지 않고 남아 있는 것만 처리
    public void shutdown() throws InterruptedException {
        shutdown = true;
        for (int i = 0; i < threads.length; i++) {
            queue.add(LAST_TASK);
        }

        for (Thread t : threads) {
            while (t.isAlive()) {
                try {
                    t.join();
                } catch (InterruptedException ignored) {
                }
            }
        }
    }
}
