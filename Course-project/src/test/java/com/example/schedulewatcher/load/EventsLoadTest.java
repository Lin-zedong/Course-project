package com.example.schedulewatcher.load;

import com.example.schedulewatcher.model.Event;
import com.example.schedulewatcher.model.Subject;
import com.example.schedulewatcher.model.SubjectType;
import com.example.schedulewatcher.repository.EventRepository;
import com.example.schedulewatcher.repository.SubjectRepository;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class EventsLoadTest {

    @LocalServerPort
    int port;

    @Autowired
    TestRestTemplate rest;

    @Autowired
    SubjectRepository subjects;

    @Autowired
    EventRepository events;

    /**
     * 负载测试：并发访问 /events，统计 RPS 和 p95 延迟。
     *
     * 标记为 @Disabled，避免每次 mvn test 都跑很久。
     * 想真正做压力测试的时候，只要去掉 @Disabled 注解重新运行。
     */
    @Test
    //@Disabled("Run manually when you need load testing")
    @Transactional
    void eventsEndpoint_handlesParallelLoad() throws Exception {
        // 1) 准备少量数据，让 /events 页面不是空的
        Subject subj = new Subject();
        subj.setName("Load Test Course");
        subj.setRuzKey("G-LOAD");
        subj.setType(SubjectType.COURSE);
        subj = subjects.save(subj);

        for (int i = 0; i < 10; i++) {
            Event e = new Event();
            e.setSubject(subj);
            e.setHash("evt-" + i);
            e.setEventTime(OffsetDateTime.now().minusMinutes(i));
            e.setDiff("{\"room\":[\"A101\",\"B202\"]}");
            events.save(e);
        }

        String url = "http://localhost:" + port + "/events";

        int threads = 20;   // 并发线程数
        int perThread = 50; // 每个线程请求次数，总共 1000 次

        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch latch = new CountDownLatch(threads * perThread);
        AtomicInteger okCount = new AtomicInteger();
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());

        long start = System.currentTimeMillis();

        for (int t = 0; t < threads; t++) {
            pool.submit(() -> {
                for (int i = 0; i < perThread; i++) {
                    long t0 = System.nanoTime();
                    var resp = rest.getForEntity(url, String.class);
                    long t1 = System.nanoTime();

                    if (resp.getStatusCode().is2xxSuccessful()) {
                        okCount.incrementAndGet();
                    }
                    latencies.add(t1 - t0);
                    latch.countDown();
                }
            });
        }

        // 最多等 60 秒
        if (!latch.await(60, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Load test did not finish in time");
        }
        pool.shutdown();

        long elapsedMs = System.currentTimeMillis() - start;
        int total = threads * perThread;
        double rps = total / (elapsedMs / 1000.0);

        // 计算 p95
        List<Long> copied = new ArrayList<>(latencies);
        Collections.sort(copied);
        long p95Ns = copied.get((int) (copied.size() * 0.95) - 1);
        double p95Ms = p95Ns / 1_000_000.0;

        System.out.printf("Total=%d, OK=%d, time=%d ms, RPS=%.2f, p95=%.2f ms%n",
                total, okCount.get(), elapsedMs, rps, p95Ms);

        // 断言：所有请求都成功（HTTP 200）
        assertEquals(total, okCount.get());
        // 是否要对 RPS / p95 做硬性限制可以自己决定，
        // 例如：assertTrue(p95Ms < 300) 对齐你报告中的 P95 目标。
    }
}
