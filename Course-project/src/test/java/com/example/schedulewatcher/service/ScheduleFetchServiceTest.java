package com.example.schedulewatcher.service;

import com.example.schedulewatcher.model.*;
import com.example.schedulewatcher.repository.*;
import com.example.schedulewatcher.util.ScheduleDiffUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ScheduleFetchServiceTest {

    @Mock SubscriptionRepository subs;
    @Mock SnapshotRepository snaps;
    @Mock EventRepository events;
    @Mock NotificationService notifier;
    @Mock RuzClient ruz;

    @InjectMocks ScheduleFetchService service;

    @Test
    void fetchAll_doesNothing_whenNoSubscriptions() {
        when(subs.findAll()).thenReturn(List.of());

        service.fetchAll();

        verifyNoInteractions(ruz, snaps, events, notifier);
    }

    @Test
    void fetchAll_createsEventAndSnapshot_whenScheduleChanged() {
        Subject subject = new Subject();
        subject.setId(1L);
        subject.setName("Math");
        subject.setRuzKey("G-1");
        subject.setType(SubjectType.COURSE);

        Subscription s = new Subscription();
        s.setSubject(subject);

        when(subs.findAll()).thenReturn(List.of(s));
        when(subs.findAllBySubjectIdFetchUser(1L)).thenReturn(List.of(s));

        String json = "[{\"lesson\":\"Math\"}]";
        when(ruz.fetchScheduleJson("COURSE", "G-1")).thenReturn(json);
        when(snaps.findTopBySubjectOrderBySnapshotAtDesc(subject))
                .thenReturn(Optional.empty());

        service.fetchAll();

        verify(events).insertIgnore(
                any(OffsetDateTime.class),
                anyString(),
                any(OffsetDateTime.class),
                anyString(),
                eq(1L)
        );
        verify(snaps).save(any(Snapshot.class));
        verify(notifier).notifyByEmail(eq(List.of(s)), any(Event.class));
    }

    @Test
    void fetchAll_skipsWhenHashSameAsLastSnapshot() {
        Subject subject = new Subject();
        subject.setId(1L);
        subject.setName("Math");
        subject.setRuzKey("G-1");
        subject.setType(SubjectType.COURSE);

        Subscription s = new Subscription();
        s.setSubject(subject);

        String json = "[{\"lesson\":\"Math\"}]";
        String hash = ScheduleDiffUtil.sha256(json);

        Snapshot last = new Snapshot();
        last.setSubject(subject);
        last.setRaw(json);
        last.setPayloadHash(hash);

        when(subs.findAll()).thenReturn(List.of(s));
        when(subs.findAllBySubjectIdFetchUser(1L)).thenReturn(List.of(s));
        when(ruz.fetchScheduleJson("COURSE", "G-1")).thenReturn(json);
        when(snaps.findTopBySubjectOrderBySnapshotAtDesc(subject))
                .thenReturn(Optional.of(last));

        service.fetchAll();

        verify(events, never()).insertIgnore(any(), any(), any(), any(), anyLong());
        verify(notifier, never()).notifyByEmail(anyList(), any());
    }
}
