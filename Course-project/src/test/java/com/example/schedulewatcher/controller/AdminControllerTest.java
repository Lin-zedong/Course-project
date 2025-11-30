package com.example.schedulewatcher.controller;

import com.example.schedulewatcher.model.Subject;
import com.example.schedulewatcher.model.SubjectType;
import com.example.schedulewatcher.model.Subscription;
import com.example.schedulewatcher.model.User;
import com.example.schedulewatcher.repository.EventRepository;
import com.example.schedulewatcher.repository.SubscriptionRepository;
import com.example.schedulewatcher.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    UserRepository users;

    @Mock
    SubscriptionRepository subs;

    @Mock
    EventRepository events;

    AdminController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminController(users, subs, events);
    }

    @Test
    void manage_populatesCountsAndRows() {
        Model model = new ExtendedModelMap();

        when(users.count()).thenReturn(3L);
        when(subs.count()).thenReturn(5L);
        when(events.countByEventTimeAfter(any(OffsetDateTime.class))).thenReturn(7L);

        Subscription courseSub = new Subscription();
        courseSub.setId(1L);
        User u1 = new User();
        u1.setEmail("course@example.com");
        courseSub.setUser(u1);
        Subject course = new Subject();
        course.setName("Course A");
        course.setRuzKey("C-1");
        course.setType(SubjectType.COURSE);
        courseSub.setSubject(course);
        courseSub.setFilters("not-json");

        Subscription teacherSub = new Subscription();
        teacherSub.setId(2L);
        User u2 = new User();
        u2.setEmail("teacher@example.com");
        teacherSub.setUser(u2);
        Subject teacher = new Subject();
        teacher.setName("Teacher B");
        teacher.setRuzKey("T-1");
        teacher.setType(SubjectType.TEACHER);
        teacherSub.setSubject(teacher);
        teacherSub.setFilters("{\"room\":\"201\",\"date\":\"2024-01-01\",\"from\":\"09:00\",\"to\":\"10:00\",\"teacher\":\"Smith\"}");

        when(subs.findAll()).thenReturn(List.of(courseSub, teacherSub));

        String view = controller.manage(model);

        assertEquals("admin/manage", view);
        assertEquals(3L, model.getAttribute("userCount"));
        assertEquals(5L, model.getAttribute("subscriptionCount"));
        assertEquals(7L, model.getAttribute("eventsToday"));

        List<?> courseRows = (List<?>) model.getAttribute("courseRows");
        List<?> teacherRows = (List<?>) model.getAttribute("teacherRows");
        assertNotNull(courseRows);
        assertNotNull(teacherRows);
        assertEquals(1, courseRows.size());
        assertEquals(1, teacherRows.size());

        AdminController.Row cr = (AdminController.Row) courseRows.get(0);
        assertEquals("course@example.com", cr.getUserEmail());
        assertEquals("Course A", cr.getSubjectName());
        assertEquals("C-1", cr.getCode());

        assertEquals("not-json", cr.getPretty());
        assertEquals(SubjectType.COURSE, cr.getType());

        AdminController.Row tr = (AdminController.Row) teacherRows.get(0);
        assertEquals("teacher@example.com", tr.getUserEmail());
        assertEquals("Teacher B", tr.getSubjectName());
        assertEquals("T-1", tr.getCode());
        assertEquals(SubjectType.TEACHER, tr.getType());
        assertNotNull(tr.getPretty());
        assertFalse(tr.getPretty().isBlank());
    }

    @Test
    void dashboard_redirectsToManage() {
        Model model = new ExtendedModelMap();
        String view = controller.dashboard(model);
        assertEquals("redirect:/admin/manage", view);
    }
}
