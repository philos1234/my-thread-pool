package threadpool;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import theadpool.MyThreadPool;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicReference;

public class MyThreadPoolTest {

    @Nested
    class ExecuteTest {
        @Test
        public void test_execute() throws InterruptedException {
            MyThreadPool sut = new MyThreadPool(3);
            CountDownLatch latch = new CountDownLatch(3);

            for (int i = 0; i < 3; i++) {
                sut.execute(() -> {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }

                    latch.countDown();
                });

            }

            latch.await();
        }
    }

    @Nested
    class ShutDownTest {

        @DisplayName("task queue 의 모든 task 가 소진될 때 까지 대기")
        @Test
        public void wait_until_queue_drains() throws InterruptedException {
            MyThreadPool sut = new MyThreadPool(2);
            for (int i = 0; i < 5; i++) {
                sut.execute(task(i));
            }

            sut.shutdown();
        }

        @DisplayName("shut down 과 execute 동시 발생하는 케이스, 큐에 넣은 task를 꺼내고 취소한다")
        @Test
        public void throw_exception_when_shutdown_is_true() throws InterruptedException {
            MyThreadPool sut = new MyThreadPool(2);
            CyclicBarrier cb = new CyclicBarrier(2);
            AtomicReference<Exception> executeException = new AtomicReference<>();

            Thread executeThread = new Thread(() -> {
                try {
                    cb.await(); // 동기화 지점
                    sut.execute(task(1));
                } catch (Exception e) {
                    executeException.set(e);
                }
            });

            Thread shutdownThread = new Thread(() -> {
                try {
                    cb.await(); // 동기화 지점
                    sut.shutdown();
                } catch (Exception ignored) {
                }
            });

            executeThread.start();
            shutdownThread.start();
            executeThread.join();
            shutdownThread.join();

            if (executeException.get() != null) {
                Assertions.assertInstanceOf(RejectedExecutionException.class,
                        executeException.get());
            }
        }


        private Runnable task(int taskNum) {
            return () -> {
                try {
                    Thread.sleep(1000);
                    System.out.printf("task %d has done\n", taskNum);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            };
        }
    }
}
