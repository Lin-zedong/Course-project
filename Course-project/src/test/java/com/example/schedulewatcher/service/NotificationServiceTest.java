package com.example.schedulewatcher.service;

import com.example.schedulewatcher.model.AuditLog;
import com.example.schedulewatcher.model.Event;
import com.example.schedulewatcher.model.Subject;
import com.example.schedulewatcher.model.Subscription;
import com.example.schedulewatcher.model.User;
import com.example.schedulewatcher.repository.AuditLogRepository;
import com.example.schedulewatcher.repository.SubjectRepository;
import com.example.schedulewatcher.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock JavaMailSender mailSender;
    @Mock AuditLogRepository audit;
    @Mock UserRepository users;
    @Mock SubjectRepository subjects;

    @InjectMocks NotificationService service;

    @Test
    void notifyByEmail_skipsWhenDiffNotMeaningful() {
        Subscription s = new Subscription();
        User u = new User();
        u.setEmail("u@example.com");
        s.setUser(u);

        Event evt = new Event();
        // type = initial → isMeaningfulDiff 应该返回 false
        evt.setDiff("{\"type\":\"initial\"}");

        service.notifyByEmail(List.of(s), evt);

        verifyNoInteractions(mailSender);
        verifyNoInteractions(audit);
    }

    @Test
    void notifyByEmail_sendsMailAndWritesAudit_whenDiffMeaningful() {
        Subscription s = new Subscription();
        User u = new User();
        u.setEmail("u@example.com");
        s.setUser(u);

        Subject subj = new Subject();
        subj.setName("Algorithms");
        s.setSubject(subj);

        Event evt = new Event();
        evt.setSubject(subj);
        evt.setDiff("{\"type\":\"changed\",\"changes\":{\"room\":{\"from\":\"101\",\"to\":\"303\"}}}");

        service.notifyByEmail(List.of(s), evt);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage msg = captor.getValue();

        assertEquals("u@example.com", msg.getTo()[0]);
        assertTrue(msg.getSubject().contains("Algorithms"));

        verify(audit).save(any(AuditLog.class));
    }

    @Test
    void formatDiffPlain_returnsEmpty_whenNullOrBlank() {
        assertEquals("", service.formatDiffPlain(null));
        assertEquals("", service.formatDiffPlain("   "));
    }

    @Test
    void formatDiffPlain_formatsChangesNicely() {
        String diffJson = "{"
                + "\"type\":\"changed\","
                + "\"changes\":{"
                + "  \"room\":{\"from\":\"101\",\"to\":\"303\"},"
                + "  \"teacher\":{\"from\":null,\"to\":\"Prof\"}"
                + "}}";

        String text = service.formatDiffPlain(diffJson);

        assertTrue(text.contains("room"));
        assertTrue(text.contains("101"));
        assertTrue(text.contains("303"));
        assertTrue(text.contains("teacher"));
        assertTrue(text.contains("Prof"));
    }
}
