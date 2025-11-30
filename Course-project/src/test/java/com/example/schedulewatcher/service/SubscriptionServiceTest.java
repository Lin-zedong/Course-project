package com.example.schedulewatcher.service;

import com.example.schedulewatcher.model.Subject;
import com.example.schedulewatcher.model.SubjectType;
import com.example.schedulewatcher.model.Subscription;
import com.example.schedulewatcher.model.User;
import com.example.schedulewatcher.repository.SubjectRepository;
import com.example.schedulewatcher.repository.SubscriptionRepository;
import com.example.schedulewatcher.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    SubscriptionRepository subs;

    @Mock
    SubjectRepository subjects;

    @Mock
    UserRepository users;

    @InjectMocks
    SubscriptionService service;

    @Test
    void listForUserDelegatesToRepository() {
        Long userId = 100L;
        List<Subscription> expected = List.of(new Subscription(), new Subscription());
        when(subs.findAllByUser_Id(userId)).thenReturn(expected);

        List<Subscription> result = service.listForUser(userId);

        assertSame(expected, result);
        verify(subs).findAllByUser_Id(userId);
        verifyNoMoreInteractions(subs);
    }

    @Test
    void addByInput_createsNewTeacherSubscription() {
        Long userId = 1L;
        String code = "T-123";

        // parse("T-123") → 新的 TEACHER subject
        Subject subject = new Subject();
        subject.setId(10L);
        subject.setRuzKey("T-123");
        subject.setType(SubjectType.TEACHER);
        when(subjects.findByRuzKey("T-123")).thenReturn(Optional.empty());
        when(subjects.save(any(Subject.class))).thenReturn(subject);

        when(subs.existsByUser_IdAndSubject_Id(userId, 10L))
                .thenReturn(false);

        User user = new User();
        user.setId(userId);
        when(users.findById(userId)).thenReturn(Optional.of(user));

        when(subs.save(any(Subscription.class))).thenAnswer(invocation -> {
            Subscription s = invocation.getArgument(0);
            s.setId(99L);
            return s;
        });

        var createdOpt = service.addByInput(userId, code);

        assertTrue(createdOpt.isPresent());
        Subscription created = createdOpt.get();
        assertEquals(99L, created.getId());

        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subs).save(captor.capture());
        Subscription toSave = captor.getValue();
        assertSame(user, toSave.getUser());
        assertSame(subject, toSave.getSubject());
        assertNotNull(toSave.getChannels());
    }

    @Test
    void addByInput_returnsEmpty_whenAlreadySubscribed() {
        Long userId = 1L;
        String code = "T-123";

        Subject subject = new Subject();
        subject.setId(10L);
        subject.setRuzKey("T-123");
        when(subjects.findByRuzKey("T-123")).thenReturn(Optional.of(subject));
        when(subs.existsByUser_IdAndSubject_Id(userId, 10L))
                .thenReturn(true);

        var result = service.addByInput(userId, code);

        assertTrue(result.isEmpty());
        verify(subs, never()).save(any());
    }

    @Test
    void addByInput_throwsOnEmptyCode() {
        assertThrows(IllegalArgumentException.class,
                () -> service.addByInput(1L, "  "));
    }

    @Test
    void delete_removesSubscription_whenBelongsToUser() {
        Long userId = 1L;
        Long subId = 10L;

        User user = new User();
        user.setId(userId);
        Subscription s = new Subscription();
        s.setId(subId);
        s.setUser(user);

        when(subs.findById(subId)).thenReturn(Optional.of(s));

        boolean deleted = service.delete(subId, userId);

        assertTrue(deleted);
        verify(subs).delete(s);
    }

    @Test
    void toggleImportant_flipsFlag_whenUserOwnsSubscription() {
        Long userId = 1L;
        Long subId = 10L;

        User user = new User();
        user.setId(userId);
        Subscription s = new Subscription();
        s.setId(subId);
        s.setUser(user);
        s.setImportant(false);

        when(subs.findById(subId)).thenReturn(Optional.of(s));

        boolean result = service.toggleImportant(subId, userId);

        assertTrue(result);
        assertTrue(s.isImportant());
        verify(subs).save(s);
    }

    @Test
    void addByInput_parsesCourseWithRoomDateAndTime() {
        Long userId = 1L;
        // name-room-YYYY-MM-DD-HH.mm-HH.mm
        String code = "Algorithms-B101-2024-01-15-9.30-11.00";

        // subjects.findByRuzKey -> 不存在 → 会走 save 新建课程
        when(subjects.findByRuzKey("Algorithms")).thenReturn(Optional.empty());
        when(subjects.save(any(Subject.class))).thenAnswer(invocation -> {
            Subject s = invocation.getArgument(0);
            s.setId(20L);
            return s;
        });

        when(subs.existsByUser_IdAndSubject_Id(userId, 20L)).thenReturn(false);

        User user = new User();
        user.setId(userId);
        when(users.findById(userId)).thenReturn(Optional.of(user));

        when(subs.save(any(Subscription.class))).thenAnswer(invocation -> {
            Subscription s = invocation.getArgument(0);
            s.setId(200L);
            return s;
        });

        var createdOpt = service.addByInput(userId, code);

        assertTrue(createdOpt.isPresent());
        Subscription created = createdOpt.get();
        assertEquals(200L, created.getId());

        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subs).save(captor.capture());
        Subscription saved = captor.getValue();

        assertSame(user, saved.getUser());
        assertNotNull(saved.getSubject());
        assertEquals("Algorithms", saved.getSubject().getName());
        assertEquals(SubjectType.COURSE, saved.getSubject().getType());

        String filters = saved.getFilters();
        assertTrue(filters.contains("\"room\":\"B101\""));
        assertTrue(filters.contains("\"date\":\"2024-01-15\""));
        assertTrue(filters.contains("\"from\""));
        assertTrue(filters.contains("\"to\""));
    }

    @Test
    void addByInput_parsesCourseWithDateAndTimeOnly() {
        Long userId = 1L;
        String code = "Physics-2024-01-20-10.00-12.00";

        when(subjects.findByRuzKey("Physics")).thenReturn(Optional.empty());
        when(subjects.save(any(Subject.class))).thenAnswer(invocation -> {
            Subject s = invocation.getArgument(0);
            s.setId(30L);
            return s;
        });

        when(subs.existsByUser_IdAndSubject_Id(userId, 30L)).thenReturn(false);

        User user = new User();
        user.setId(userId);
        when(users.findById(userId)).thenReturn(Optional.of(user));

        when(subs.save(any(Subscription.class))).thenAnswer(invocation -> {
            Subscription s = invocation.getArgument(0);
            s.setId(300L);
            return s;
        });

        var createdOpt = service.addByInput(userId, code);

        assertTrue(createdOpt.isPresent());

        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subs).save(captor.capture());
        Subscription saved = captor.getValue();

        String filters = saved.getFilters();
        assertFalse(filters.contains("room")); // 这个格式没有教室
        assertTrue(filters.contains("\"date\":\"2024-01-20\""));
        assertTrue(filters.contains("\"from\""));
        assertTrue(filters.contains("\"to\""));
    }

    @Test
    void addByInput_parsesCourseWithRoomOnly() {
        Long userId = 1L;
        String code = "Chemistry-C303";

        when(subjects.findByRuzKey("Chemistry")).thenReturn(Optional.empty());
        when(subjects.save(any(Subject.class))).thenAnswer(invocation -> {
            Subject s = invocation.getArgument(0);
            s.setId(40L);
            return s;
        });

        when(subs.existsByUser_IdAndSubject_Id(userId, 40L)).thenReturn(false);

        User user = new User();
        user.setId(userId);
        when(users.findById(userId)).thenReturn(Optional.of(user));

        when(subs.save(any(Subscription.class))).thenAnswer(invocation -> {
            Subscription s = invocation.getArgument(0);
            s.setId(400L);
            return s;
        });

        var createdOpt = service.addByInput(userId, code);

        assertTrue(createdOpt.isPresent());

        ArgumentCaptor<Subscription> captor = ArgumentCaptor.forClass(Subscription.class);
        verify(subs).save(captor.capture());
        Subscription saved = captor.getValue();

        String filters = saved.getFilters();
        assertEquals("{\"room\":\"C303\"}", filters);
    }
}
