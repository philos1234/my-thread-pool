package threadpool;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;

public class TestClass {

    @Test
    public void test() throws BrokenBarrierException, InterruptedException {
        ConcurrentHashMap<String, Integer> m = new ConcurrentHashMap<>();

        for (int i = 0; i < 500; i++) {
            m.put(STR."key\{i}", i);
        }

        CyclicBarrier cb = new CyclicBarrier(3);

        List<Integer> t1Ret = new ArrayList<>();
        Thread t1 = new Thread(() -> {
            try {
                cb.await();
                drainTo(m, t1Ret);
            } catch (InterruptedException | BrokenBarrierException e) {
                throw new RuntimeException(e);
            }
        });


        List<Integer> t2Ret = new ArrayList<>();
        Thread t2 = new Thread(() -> {
            try {
                cb.await();
                drainTo(m, t2Ret);
            } catch (InterruptedException | BrokenBarrierException e) {
                throw new RuntimeException(e);
            }
        });


        t1.start();
        t2.start();

        cb.await();
        System.out.println("t1  result");
        print(t1Ret);
        System.out.print("\n");
        System.out.println("t2  result");
        print(t2Ret);
    }

    private void drainTo(ConcurrentHashMap<String, Integer> m, List<Integer> ret) {
        for (Map.Entry<String, Integer> e : m.entrySet()) {
            Integer value = m.remove(e.getKey());
            if (value != null) {
                ret.add(value);
            }
        }
    }

    private void print(List<Integer> r) {
        for (Integer e : r) {
            System.out.printf(String.format("%d, ", e));
        }
    }
}
