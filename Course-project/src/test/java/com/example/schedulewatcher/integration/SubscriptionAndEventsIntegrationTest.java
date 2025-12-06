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
@ActiveProfiles("test")   
@Transactional
class SubscriptionAndEventsIntegrationTest {

    @Autowired MockMvc mockMvc;

    @Autowired UserRepository userRepository;
    @Autowired SubjectRepository subjectRepository;
    @Autowired SubscriptionRepository subscriptionRepository;
    @Autowired EventRepository eventRepository;

    
    @Test
    void eventsPage_showsOnlyEventsForCurrentUserSubscriptions() throws Exception {
        
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
        e2.setSubject(foreign); 
        e2.setEventTime(OffsetDateTime.now().minusHours(2));
        e2.setHash("chem-hash");
        e2.setDiff("{\"room\":[\"C303\",\"D404\"]}");
        eventRepository.save(e2);

        
        MockHttpSession session = new MockHttpSession();
        session.setAttribute("uid", student.getId().toString());

        
        mockMvc.perform(get("/events").session(session))
                .andExpect(status().isOk())
                .andExpect(view().name("events"))
                .andExpect(model().attributeExists("events"))
                // 页面里应该出现 Algorithms，但不应该出现 Chemistry
                .andExpect(content().string(containsString("Algorithms")))
                .andExpect(content().string(not(containsString("Chemistry"))));
    }

    
    @Test
    void student_canToggleImportantFlagThroughController() throws Exception {
        
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

        
        mockMvc.perform(post("/subscriptions/{id}/important", sub.getId())
                        .session(session)
                        .with(csrf()))     
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/subscriptions"));

        
        Subscription reloaded = subscriptionRepository.findById(sub.getId())
                .orElseThrow();
        assertTrue(reloaded.isImportant(), "Subscription should be marked as important");
    }
}

