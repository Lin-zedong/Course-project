package com.example.schedulewatcher.controller;

import com.example.schedulewatcher.model.Subject;
import com.example.schedulewatcher.model.SubjectType;
import com.example.schedulewatcher.model.Subscription;
import com.example.schedulewatcher.model.User;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionControllerTest {

    @Mock
    SubscriptionService svc;

    @Mock
    UserSessionService userSession;

    SubscriptionController controller;

    @BeforeEach
    void setUp() {
        controller = new SubscriptionController(svc, userSession);
    }

    @Test
    void page_populatesModelWithSubsView() {
        HttpSession session = mock(HttpSession.class);
        Model model = new ExtendedModelMap();

        User me = new User();
        me.setId(1L);
        me.setEmail("me@example.com");
        when(userSession.current(session)).thenReturn(me);

        Subject subject = new Subject();
        subject.setName("Algorithms");
        subject.setRuzKey("G-01");
        subject.setType(SubjectType.COURSE);

        Subscription sub = new Subscription();
        sub.setId(10L);
        sub.setSubject(subject);
        sub.setImportant(true);
        sub.setFilters("{\"room\":\"101\",\"date\":\"2024-01-01\",\"from\":\"10:00\",\"to\":\"12:00\"}");

        when(svc.listForUser(1L)).thenReturn(List.of(sub));

        String view = controller.page(session, model);

        assertEquals("subscriptions", view);
        assertEquals("me@example.com", model.getAttribute("email"));

        Object subsViewObj = model.getAttribute("subsView");
        assertNotNull(subsViewObj);
        assertTrue(subsViewObj instanceof List<?>);
        List<?> list = (List<?>) subsViewObj;
        assertEquals(1, list.size());
        Object first = list.get(0);
        assertTrue(first instanceof SubscriptionController.SubsView);
        SubscriptionController.SubsView vm = (SubscriptionController.SubsView) first;
        assertEquals(sub.getId(), vm.getId());
        assertEquals("Algorithms", vm.getSubjectName());
        assertEquals("G-01", vm.getCode());
        assertTrue(vm.isImportant());
        assertNotNull(vm.getFormattedSession());
        assertFalse(vm.getFormattedSession().isBlank());
    }

    @Test
    void add_createsSubscriptionAndSetsOkMessage_whenNotExisting() {
        HttpSession session = mock(HttpSession.class);
        RedirectAttributes ra = mock(RedirectAttributes.class);

        User me = new User();
        me.setId(1L);
        when(userSession.current(session)).thenReturn(me);

        Map<String, String> form = new HashMap<>();
        form.put("code", "T-123");

        when(svc.addByInput(1L, "T-123"))
                .thenReturn(Optional.of(new Subscription()));

        String view = controller.add(session, form, ra);

        assertEquals("redirect:/subscriptions", view);
        verify(svc).addByInput(1L, "T-123");
        verify(ra).addFlashAttribute("ok", "Subscription added");
    }

    @Test
    void add_setsAlreadySubscribedMessage_whenServiceReturnsEmpty() {
        HttpSession session = mock(HttpSession.class);
        RedirectAttributes ra = mock(RedirectAttributes.class);

        User me = new User();
        me.setId(1L);
        when(userSession.current(session)).thenReturn(me);

        Map<String, String> form = Map.of("code", "T-123");
        when(svc.addByInput(1L, "T-123")).thenReturn(Optional.empty());

        controller.add(session, form, ra);

        verify(ra).addFlashAttribute("ok", "Already subscribed");
    }

    @Test
    void add_setsErrorMessage_whenIllegalArgument() {
        HttpSession session = mock(HttpSession.class);
        RedirectAttributes ra = mock(RedirectAttributes.class);

        User me = new User();
        me.setId(1L);
        when(userSession.current(session)).thenReturn(me);

        Map<String, String> form = Map.of("code", "bad");
        when(svc.addByInput(1L, "bad")).thenThrow(new IllegalArgumentException("Bad input"));

        controller.add(session, form, ra);

        verify(ra).addFlashAttribute("err", "Bad input");
    }

    @Test
    void add_setsGenericError_whenOtherException() {
        HttpSession session = mock(HttpSession.class);
        RedirectAttributes ra = mock(RedirectAttributes.class);

        User me = new User();
        me.setId(1L);
        when(userSession.current(session)).thenReturn(me);

        Map<String, String> form = Map.of("code", "boom");
        when(svc.addByInput(1L, "boom")).thenThrow(new RuntimeException("boom"));

        controller.add(session, form, ra);

        verify(ra).addFlashAttribute("err", "Failed to add subscription");
    }

    @Test
    void add_setsErrorWhenNothingSubmitted() {
        HttpSession session = mock(HttpSession.class);
        RedirectAttributes ra = mock(RedirectAttributes.class);

        User me = new User();
        me.setId(1L);
        when(userSession.current(session)).thenReturn(me);

        Map<String, String> form = Map.of(); // empty map

        controller.add(session, form, ra);

        verify(ra).addFlashAttribute("err", "Nothing submitted");
        verifyNoInteractions(svc);
    }

    @Test
    void delete_setsOkMessage_whenDeleted() {
        HttpSession session = mock(HttpSession.class);
        RedirectAttributes ra = mock(RedirectAttributes.class);

        User me = new User();
        me.setId(1L);
        when(userSession.current(session)).thenReturn(me);

        when(svc.delete(10L, 1L)).thenReturn(true);

        String view = controller.delete(session, 10L, ra);

        assertEquals("redirect:/subscriptions", view);
        verify(ra).addFlashAttribute("ok", "Deleted");
    }

    @Test
    void delete_setsErrMessage_whenNotDeleted() {
        HttpSession session = mock(HttpSession.class);
        RedirectAttributes ra = mock(RedirectAttributes.class);

        User me = new User();
        me.setId(1L);
        when(userSession.current(session)).thenReturn(me);

        when(svc.delete(10L, 1L)).thenReturn(false);

        controller.delete(session, 10L, ra);

        verify(ra).addFlashAttribute("err", "Not found");
    }

    @Test
    void toggleImportant_setsOk_whenUpdated() {
        HttpSession session = mock(HttpSession.class);
        RedirectAttributes ra = mock(RedirectAttributes.class);

        User me = new User();
        me.setId(1L);
        when(userSession.current(session)).thenReturn(me);

        when(svc.toggleImportant(10L, 1L)).thenReturn(true);

        String view = controller.toggleImportant(session, 10L, ra);

        assertEquals("redirect:/subscriptions", view);
        verify(ra).addFlashAttribute("ok", "Updated");
    }

    @Test
    void toggleImportant_setsErr_whenNotUpdated() {
        HttpSession session = mock(HttpSession.class);
        RedirectAttributes ra = mock(RedirectAttributes.class);

        User me = new User();
        me.setId(1L);
        when(userSession.current(session)).thenReturn(me);

        when(svc.toggleImportant(10L, 1L)).thenReturn(false);

        controller.toggleImportant(session, 10L, ra);

        verify(ra).addFlashAttribute("err", "Not found");
    }
}
