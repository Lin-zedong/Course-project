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

    
    @Test
    
    @Transactional
    void eventsEndpoint_handlesParallelLoad() throws Exception {
        
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

        int threads = 20;   
        int perThread = 50; 

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

        
        if (!latch.await(60, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Load test did not finish in time");
        }
        pool.shutdown();

        long elapsedMs = System.currentTimeMillis() - start;
        int total = threads * perThread;
        double rps = total / (elapsedMs / 1000.0);

        
        List<Long> copied = new ArrayList<>(latencies);
        Collections.sort(copied);
        long p95Ns = copied.get((int) (copied.size() * 0.95) - 1);
        double p95Ms = p95Ns / 1_000_000.0;

        System.out.printf("Total=%d, OK=%d, time=%d ms, RPS=%.2f, p95=%.2f ms%n",
                total, okCount.get(), elapsedMs, rps, p95Ms);

        
        assertEquals(total, okCount.get());
    }
}

