package com.example.schedulewatcher.service;

import com.example.schedulewatcher.model.User;
import com.example.schedulewatcher.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class UserSessionService {
    private static final String KEY = "uid";
    private final UserRepository users;

    public UserSessionService(UserRepository users) {
        this.users = users;
    }

    public User current(HttpSession session) {
        Object idObj = session.getAttribute(KEY);
        if (idObj != null) {
            try {
                Long id = Long.parseLong(idObj.toString());
                return users.findById(id).orElseGet(() -> createAndBind(session));
            } catch (NumberFormatException ignore) {
                return createAndBind(session);
            }
        }
        return createAndBind(session);
    }

    private User createAndBind(HttpSession session) {
        User u = new User();
        users.save(u);
        session.setAttribute(KEY, u.getId().toString());
        return u;
    }

    @Transactional
    public void updateEmail(HttpSession session, String email) {
        User me = current(session);

        Optional<User> existed = users.findByEmail(email);
        if (existed.isPresent() && !existed.get().getId().equals(me.getId())) {
            session.setAttribute(KEY, existed.get().getId().toString());
            return;
        }

        me.setEmail(email);
        users.save(me);
    }
}
