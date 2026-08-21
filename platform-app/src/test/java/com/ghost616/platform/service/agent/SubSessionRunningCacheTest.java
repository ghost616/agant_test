package com.ghost616.platform.service.agent;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SubSessionRunningCacheTest {

    private final SubSessionRunningCache cache = new SubSessionRunningCache();

    @Test
    void addAndContains() {
        assertFalse(cache.contains(1L));

        cache.add(1L);

        assertTrue(cache.contains(1L));
    }

    @Test
    void removeAfterAdd() {
        cache.add(1L);

        cache.remove(1L);

        assertFalse(cache.contains(1L));
    }

    @Test
    void removeNonExistentIsNoOp() {
        cache.remove(999L);

        assertFalse(cache.contains(999L));
    }

    @Test
    void nullSafety() {
        assertFalse(cache.contains(null));

        cache.add(null);
        cache.remove(null);

        assertFalse(cache.contains(null));
    }

    @Test
    void capacityLimitClearsWholeCache() {
        // 填满容量上限（MAX_CACHE_SIZE=10000）
        for (long i = 0; i < 10000; i++) {
            cache.add(i);
        }
        assertTrue(cache.contains(0L));

        // 再添加一条触发超限清空：旧条目全部失效，仅保留新增条目
        cache.add(10000L);

        assertTrue(cache.contains(10000L));
        assertFalse(cache.contains(0L));
    }

    @Test
    void threadSafetyBasicBehavior() throws Exception {
        int threads = 8;
        int perThread = 500;
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int t = 0; t < threads; t++) {
                final int base = t * perThread;
                futures.add(executor.submit(() -> {
                    start.await();
                    for (int i = 0; i < perThread; i++) {
                        long id = base + i;
                        cache.add(id);
                        cache.contains(id);
                        cache.remove(id);
                    }
                    return null;
                }));
            }
            start.countDown();
            for (Future<?> future : futures) {
                future.get(5, TimeUnit.SECONDS);
            }
            // 各线程操作互不干扰，最终无残留条目
            for (long i = 0; i < threads * perThread; i++) {
                assertFalse(cache.contains(i));
            }
        } finally {
            executor.shutdownNow();
        }
    }
}
