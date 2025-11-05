package com.example.schedulewatcher.service;

import com.example.schedulewatcher.model.*;
import com.example.schedulewatcher.repository.*;
import com.example.schedulewatcher.util.ScheduleDiffUtil;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ScheduleFetchService {

    private final SubscriptionRepository subs;
    private final SnapshotRepository snaps;
    private final EventRepository events;
    private final NotificationService notifier;
    private final RuzClient ruz;

    public ScheduleFetchService(SubscriptionRepository subs, SnapshotRepository snaps, EventRepository events,
                                NotificationService notifier, RuzClient ruz) {
        this.subs = subs;
        this.snaps = snaps;
        this.events = events;
        this.notifier = notifier;
        this.ruz = ruz;
    }

    @Scheduled(fixedDelayString = "${fetcher.fixedDelay:60000}")
    @Transactional
    public void fetchAll() {
        // Gather subjects from subscriptions
        List<Subscription> all = subs.findAll();
        Map<Subject, List<Subscription>> bySubject = all.stream().collect(Collectors.groupingBy(Subscription::getSubject));

        for (Map.Entry<Subject, List<Subscription>> entry : bySubject.entrySet()) {
            Subject subject = entry.getKey();
            List<Subscription> sSubs = subs.findAllBySubjectIdFetchUser(subject.getId());
            String json = ruz.fetchScheduleJson(subject.getType().name(), subject.getRuzKey());
            String hash = ScheduleDiffUtil.sha256(json);
            String oldRaw = snaps.findTopBySubjectOrderBySnapshotAtDesc(subject)
                    .map(Snapshot::getRaw).orElse(null);

            if (oldRaw == null || !hash.equals(ScheduleDiffUtil.sha256(oldRaw))) {
                // Changed
                String diff = ScheduleDiffUtil.diff(oldRaw, json);
                Event evt = new Event();
                evt.setSubject(subject);
                evt.setEventTime(OffsetDateTime.now());
                evt.setDiff(diff);
                evt.setHash(hash);
                events.insertIgnore(java.time.OffsetDateTime.now(), diff, evt.getEventTime(), hash, subject.getId());

                // Save snapshot
                Snapshot snap = new Snapshot();
                snap.setSubject(subject);
                snap.setSnapshotAt(OffsetDateTime.now());
                snap.setPayloadHash(hash);
                snap.setRaw(json);
                snaps.save(snap);

                // Notify email
                notifier.notifyByEmail(sSubs, evt);
            }
        }
    }
}
