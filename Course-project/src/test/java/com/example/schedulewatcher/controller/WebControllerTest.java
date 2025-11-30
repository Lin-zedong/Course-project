package com.example.schedulewatcher.controller;

import com.example.schedulewatcher.model.User;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WebControllerTest {

    @Mock UserSessionService userSession;

    WebController controller;

    @BeforeEach
    void setUp() {
        controller = new WebController(userSession);
    }

    @Test
    void index_putsEmailAndReturnsIndex() {
        HttpSession session = mock(HttpSession.class);
        Model model = new ExtendedModelMap();

        User me = new User();
        me.setEmail("me@example.com");
        when(userSession.current(session)).thenReturn(me);

        String view = controller.index(session, model);

        assertEquals("index", view);
        assertEquals("me@example.com", model.getAttribute("email"));
    }

    @Test
    void saveEmail_updatesAndRedirects() {
        HttpSession session = mock(HttpSession.class);
        RedirectAttributes ra = mock(RedirectAttributes.class);

        String view = controller.saveEmail(session, "new@example.com", ra);

        assertEquals("redirect:/subscriptions", view);
        verify(userSession).updateEmail(session, "new@example.com");
        verify(ra).addFlashAttribute(eq("ok"), any());
    }

    @Test
    void login_returnsLoginView() {
        assertEquals("login", controller.login());
    }
}
