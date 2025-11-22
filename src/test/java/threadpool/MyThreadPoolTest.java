package threadpool;

import org.junit.jupiter.api.Test;
import theadpool.MyThreadPool;

import java.util.concurrent.CountDownLatch;

public class MyThreadPoolTest {

    @Test
    public void test() throws InterruptedException {
        MyThreadPool pool = new MyThreadPool(3);
        CountDownLatch latch = new CountDownLatch(3);

        for (int i = 0; i < 3; i++) {
            pool.execute(() -> {
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
