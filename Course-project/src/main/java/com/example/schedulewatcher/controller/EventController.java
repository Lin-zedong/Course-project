package com.example.schedulewatcher.controller;

import com.example.schedulewatcher.model.Event;
import com.example.schedulewatcher.model.Subject;
import com.example.schedulewatcher.model.Subscription;
import com.example.schedulewatcher.model.User;
import com.example.schedulewatcher.service.SubscriptionService;
import com.example.schedulewatcher.repository.EventRepository;
import com.example.schedulewatcher.repository.SubscriptionRepository;
import com.example.schedulewatcher.service.UserSessionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.stream.Collectors;

@Controller
public class EventController {

    private final EventRepository events;
    private final SubscriptionService subscriptionService;
    private final UserSessionService userSession;

    public EventController(EventRepository events, SubscriptionRepository subs, SubscriptionService subscriptionService, UserSessionService userSession) {
        this.events = events;
        this.subscriptionService = subscriptionService;
        this.userSession = userSession;
    }

    @GetMapping("/events")
    public String events(HttpSession session, Model model) {
        User user = userSession.current(session);
        List<Subscription> mySubs = subscriptionService.listForUser(user.getId());
        List<Subject> subjects = mySubs.stream().map(Subscription::getSubject).collect(Collectors.toList());
        List<Event> evts = events.findBySubjectInOrderByEventTimeDesc(subjects);
        model.addAttribute("events", evts);
        return "events";
    }
}
