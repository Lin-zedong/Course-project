package com.example.schedulewatcher.integration;

import com.example.schedulewatcher.model.*;
import com.example.schedulewatcher.repository.EventRepository;
import com.example.schedulewatcher.repository.SubjectRepository;
import com.example.schedulewatcher.repository.SubscriptionRepository;
import com.example.schedulewatcher.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")   // 使用 application-test.yml
@Transactional
class SubscriptionAndEventsIntegrationTest {

    @Autowired MockMvc mockMvc;

    @Autowired UserRepository userRepository;
    @Autowired SubjectRepository subjectRepository;
    @Autowired SubscriptionRepository subscriptionRepository;
    @Autowired EventRepository eventRepository;

    /**
     * 场景 1：学生访问 /events，只看到自己订阅科目的变更。
     */
    @Test
    void eventsPage_showsOnlyEventsForCurrentUserSubscriptions() throws Exception {
        // 1) 准备数据：一个学生 + 两个订阅 + 一些事件
        User student = new User();
        student.setEmail("student@example.com");
        student = userRepository.save(student);

        Subject algo = new Subject();
        algo.setName("Algorithms");
        algo.setRuzKey("G-ALGO");
        algo.setType(SubjectType.COURSE);
        algo = subjectRepository.save(algo);

        Subject physics = new Subject();
        physics.setName("Physics");
        physics.setRuzKey("G-PHYS");
        physics.setType(SubjectType.COURSE);
        physics = subjectRepository.save(physics);

        Subject foreign = new Subject();
        foreign.setName("Chemistry");
        foreign.setRuzKey("G-CHEM");
        foreign.setType(SubjectType.COURSE);
        foreign = subjectRepository.save(foreign);

        Subscription s1 = new Subscription();
        s1.setUser(student);
        s1.setSubject(algo);
        s1.setChannels("[\"web\"]");
        subscriptionRepository.save(s1);

        Subscription s2 = new Subscription();
        s2.setUser(student);
        s2.setSubject(physics);
        s2.setChannels("[\"web\"]");
        subscriptionRepository.save(s2);

        Event e1 = new Event();
        e1.setSubject(algo);
        e1.setEventTime(OffsetDateTime.now().minusHours(1));
        e1.setHash("algo-hash");
        e1.setDiff("{\"room\":[\"A101\",\"B202\"]}");
        eventRepository.save(e1);

        Event e2 = new Event();
        e2.setSubject(foreign); // 不是当前学生订阅的 subject
        e2.setEventTime(OffsetDateTime.now().minusHours(2));
        e2.setHash("chem-hash");
        e2.setDiff("{\"room\":[\"C303\",\"D404\"]}");
        eventRepository.save(e2);

        // 2) 模拟该学生已经在 session 里登录（UserSessionService 使用 "uid" 存 ID）
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("uid", student.getId().toString());

        // 3) 访问 /events
        mockMvc.perform(get("/events").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("events"))
                .andExpect(model().attributeExists("events"))
                // 页面里应该出现 Algorithms，但不应该出现 Chemistry
                .andExpect(content().string(containsString("Algorithms")))
                .andExpect(content().string(not(containsString("Chemistry"))));
    }

    /**
     * 场景 2：学生通过 UI 把订阅标记为重要（覆盖 US‑2 的一部分）。
     */
    @Test
    void student_canToggleImportantFlagThroughController() throws Exception {
        // 1) 一个学生 + 一条订阅，初始 important = false
        User student = new User();
        student.setEmail("student2@example.com");
        student = userRepository.save(student);

        Subject subj = new Subject();
        subj.setName("Distributed Systems");
        subj.setRuzKey("G-DS");
        subj.setType(SubjectType.COURSE);
        subj = subjectRepository.save(subj);

        Subscription sub = new Subscription();
        sub.setUser(student);
        sub.setSubject(subj);
        sub.setChannels("[\"web\"]");
        sub.setImportant(false);
        sub = subscriptionRepository.save(sub);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute("uid", student.getId().toString());

        // 2) 通过 POST /subscriptions/{id}/important 触发 toggle
        mockMvc.perform(post("/subscriptions/{id}/important", sub.getId())
                        .session(session)
                        .with(csrf()))     // 如果开启了 CSRF，这是必须的
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/subscriptions"));

        // 3) 再从数据库查一次，确认 important 已经被置为 true
        Subscription reloaded = subscriptionRepository.findById(sub.getId())
                .orElseThrow();
        assertTrue(reloaded.isImportant(), "Subscription should be marked as important");
    }
}
