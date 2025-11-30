package com.example.schedulewatcher.service;

import com.example.schedulewatcher.model.User;
import com.example.schedulewatcher.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserSessionServiceTest {

    @Mock
    UserRepository users;

    @Mock
    HttpSession session;

    @InjectMocks
    UserSessionService service;

    @Test
    void current_createsNewUser_whenSessionEmpty() {
        when(session.getAttribute("uid")).thenReturn(null);

        when(users.save(any(User.class))).thenAnswer(invocation -> {
            User u = invocation.getArgument(0);
            u.setId(42L);
            return u;
        });

        User result = service.current(session);

        assertEquals(42L, result.getId());
        verify(users).save(any(User.class));
        verify(session).setAttribute("uid", "42");
    }

    @Test
    void current_returnsExistingUser_whenValidIdInSession() {
        when(session.getAttribute("uid")).thenReturn("10");

        User existing = new User();
        existing.setId(10L);
        when(users.findById(10L)).thenReturn(Optional.of(existing));

        User result = service.current(session);

        assertSame(existing, result);

        verify(users, never()).save(any());
    }

    @Test
    void updateEmail_rebindsSession_whenEmailBelongsToOtherUser() {

        when(session.getAttribute("uid")).thenReturn("1");
        User me = new User();
        me.setId(1L);
        when(users.findById(1L)).thenReturn(Optional.of(me));

        User other = new User();
        other.setId(2L);
        when(users.findByEmail("used@example.com"))
                .thenReturn(Optional.of(other));

        service.updateEmail(session, "used@example.com");

        verify(session).setAttribute("uid", "2");

        verify(users, never()).save(me);
    }

    @Test
    void updateEmail_updatesCurrentUser_whenEmailIsFree() {
        when(session.getAttribute("uid")).thenReturn("1");
        User me = new User();
        me.setId(1L);
        when(users.findById(1L)).thenReturn(Optional.of(me));
        when(users.findByEmail("new@example.com"))
                .thenReturn(Optional.empty());

        service.updateEmail(session, "new@example.com");

        assertEquals("new@example.com", me.getEmail());
        verify(users).save(me);
    }
}
