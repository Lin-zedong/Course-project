package com.example.schedulewatcher.controller;

import com.example.schedulewatcher.model.Event;
import com.example.schedulewatcher.model.Subject;
import com.example.schedulewatcher.model.Subscription;
import com.example.schedulewatcher.model.User;
import com.example.schedulewatcher.repository.EventRepository;
import com.example.schedulewatcher.service.SubscriptionService;
import com.example.schedulewatcher.service.UserSessionService;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    @Mock EventRepository eventsRepo;
    @Mock SubscriptionService subscriptionService;
    @Mock UserSessionService userSession;

    EventController controller;

    @BeforeEach
    void setUp() {
        controller = new EventController(eventsRepo, subscriptionService, userSession);
    }

    @Test
    void events_addsEventsToModelAndReturnsViewName() {
        HttpSession session = mock(HttpSession.class);
        Model model = new ExtendedModelMap();

        User user = new User();
        user.setId(1L);
        when(userSession.current(session)).thenReturn(user);

        Subject subject = new Subject();
        subject.setId(10L);

        Subscription sub = new Subscription();
        sub.setSubject(subject);

        List<Subscription> mySubs = List.of(sub);
        when(subscriptionService.listForUser(1L)).thenReturn(mySubs);

        List<Event> evts = List.of(new Event());
        when(eventsRepo.findBySubjectInOrderByEventTimeDesc(List.of(subject)))
                .thenReturn(evts);

        String view = controller.events(session, model);

        assertEquals("events", view);
        assertSame(evts, model.getAttribute("events"));
    }
}
