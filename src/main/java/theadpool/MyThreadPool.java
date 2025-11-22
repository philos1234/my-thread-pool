package theadpool;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

public class MyThreadPool implements Executor {

    private final BlockingQueue<Runnable> queue = new LinkedBlockingQueue<>();
    private volatile Boolean isShutDown;
    private final Thread[] threads;

    private final AtomicBoolean isStarted = new AtomicBoolean();

    public MyThreadPool(int threadNums) {
        this.threads = new Thread[threadNums];
        for (int i = 0; i < threadNums; i++) {
            threads[i] = new Thread(() -> {
                while (!isShutDown) {
                    Runnable task = null;
                    try {
                        task = queue.take();
                    } catch (InterruptedException ignored) {
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

        isStarted.set(true);
    }

    @Override
    public void execute(Runnable command) {
        if (isStarted.compareAndSet(false, true)) {
            for (Thread t : threads) {
                t.start();
            }
        }

        if (isShutDown) {
            throw new RejectedExecutionException();
        }

        queue.add(command);
    }

    public void shutdown() {
        isShutDown = true;
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
