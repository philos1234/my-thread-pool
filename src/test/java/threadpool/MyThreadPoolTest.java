package threadpool;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import theadpool.MyThreadPool;

import java.util.concurrent.CountDownLatch;

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
            CountDownLatch latch = new CountDownLatch(5);

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
}
