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

    public MyThreadPool(int threadNums) {
        this.threads = new Thread[threadNums];
        for (int i = 0; i < threadNums; i++) {
            threads[i] = new Thread(() -> {
                while (!shutdown) {
                    Runnable task = null;
                    try {
                        task = queue.take();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }

                    if (task != null) {
                        try {
                            task.run();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
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

    public void shutdown() {
        shutdown = true;
        for (Thread t : threads) {
            t.interrupt();
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
