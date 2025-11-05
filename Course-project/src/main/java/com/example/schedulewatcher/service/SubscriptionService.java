package com.example.schedulewatcher.service;

import com.example.schedulewatcher.model.Subject;
import com.example.schedulewatcher.model.SubjectType;
import com.example.schedulewatcher.model.Subscription;
import com.example.schedulewatcher.model.User;
import com.example.schedulewatcher.repository.SubjectRepository;
import com.example.schedulewatcher.repository.SubscriptionRepository;
import com.example.schedulewatcher.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SubscriptionService {

    private final SubscriptionRepository subs;
    private final SubjectRepository subjects;
    private final UserRepository users;

    public SubscriptionService(SubscriptionRepository subs,
                               SubjectRepository subjects,
                               UserRepository users) {
        this.subs = subs;
        this.subjects = subjects;
        this.users = users;
    }

    private ParsedInput parse(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        if (s.isEmpty()) return null;

        Matcher mt = Pattern.compile("^[Tt][-_]?(\\d+)$").matcher(s);
        if (mt.matches()) {
            ParsedInput pi = new ParsedInput();
            pi.type = SubjectType.TEACHER;
            pi.ruzKey = "T-" + mt.group(1);
            pi.name = "Teacher " + mt.group(1);
            pi.filtersJson = "{}";
            return pi;
        }

        Matcher mcrd = Pattern.compile("^(.+)-([A-Za-z0-9]+)-(\\d{4})-(\\d{2})-(\\d{2})-(\\d{1,2})\\.(\\d{2})-(\\d{1,2})\\.(\\d{2})$")
                .matcher(s);
        if (mcrd.matches()) {
            String name = mcrd.group(1).trim();
            String room = mcrd.group(2).trim();
            LocalDate date = LocalDate.of(
                    Integer.parseInt(mcrd.group(3)),
                    Integer.parseInt(mcrd.group(4)),
                    Integer.parseInt(mcrd.group(5)));
            LocalTime from = LocalTime.of(Integer.parseInt(mcrd.group(6)), Integer.parseInt(mcrd.group(7)));
            LocalTime to   = LocalTime.of(Integer.parseInt(mcrd.group(8)), Integer.parseInt(mcrd.group(9)));

            ParsedInput pi = new ParsedInput();
            pi.type = SubjectType.COURSE;
            pi.ruzKey = name;
            pi.name = name;
            pi.filtersJson = String.format("{\"room\":\"%s\",\"date\":\"%s\",\"from\":\"%s\",\"to\":\"%s\"}",
                    escape(room), date, from, to);
            return pi;
        }

        Matcher mcd = Pattern.compile("^(.+)-(\\d{4})-(\\d{2})-(\\d{2})-(\\d{1,2})\\.(\\d{2})-(\\d{1,2})\\.(\\d{2})$")
                .matcher(s);
        if (mcd.matches()) {
            String name = mcd.group(1).trim();
            LocalDate date = LocalDate.of(
                    Integer.parseInt(mcd.group(2)),
                    Integer.parseInt(mcd.group(3)),
                    Integer.parseInt(mcd.group(4)));
            LocalTime from = LocalTime.of(Integer.parseInt(mcd.group(5)), Integer.parseInt(mcd.group(6)));
            LocalTime to   = LocalTime.of(Integer.parseInt(mcd.group(7)), Integer.parseInt(mcd.group(8)));

            ParsedInput pi = new ParsedInput();
            pi.type = SubjectType.COURSE;
            pi.ruzKey = name;
            pi.name = name;
            pi.filtersJson = String.format("{\"date\":\"%s\",\"from\":\"%s\",\"to\":\"%s\"}", date, from, to);
            return pi;
        }

        Matcher mcr = Pattern.compile("^(.+)-([A-Za-z0-9]+)$").matcher(s);
        if (mcr.matches()) {
            String name = mcr.group(1).trim();
            String room = mcr.group(2).trim();

            ParsedInput pi = new ParsedInput();
            pi.type = SubjectType.COURSE;
            pi.ruzKey = name;
            pi.name = name;
            pi.filtersJson = String.format("{\"room\":\"%s\"}", escape(room));
            return pi;
        }

        ParsedInput pi = new ParsedInput();
        pi.type = SubjectType.COURSE;
        pi.ruzKey = s;
        pi.name = s;
        pi.filtersJson = "{}";
        return pi;
    }

    private static String escape(String v) {
        return v.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    @Transactional
    public Optional<Subscription> addByInput(Long userId, String code) {
        if (code == null || code.isBlank())
            throw new IllegalArgumentException("empty code");

        ParsedInput pi = parse(code);
        if (pi == null)
            throw new IllegalArgumentException("Unknown code: " + code.trim());

        Subject subject = subjects.findByRuzKey(pi.ruzKey)
                .orElseGet(() -> {
                    Subject s = new Subject();
                    s.setName(pi.name);
                    s.setRuzKey(pi.ruzKey);
                    s.setType(pi.type);
                    return subjects.save(s);
                });

        if (subs.existsByUser_IdAndSubject_Id(userId, subject.getId())) {
            return Optional.empty();
        }

        User user = users.findById(userId).orElseThrow();

        Subscription s = new Subscription();
        s.setUser(user);
        s.setSubject(subject);
        s.setImportant(false);
        s.setCreatedAt(OffsetDateTime.now());
        s.setChannels("[\"web\",\"email\"]");
        s.setFilters(pi.filtersJson != null ? pi.filtersJson : "{}");

        return Optional.of(subs.save(s));
    }

    @Transactional
    public boolean delete(Long subId, Long userId) {
        return subs.findById(subId)
                .filter(s -> s.getUser().getId().equals(userId))
                .map(s -> { subs.delete(s); return true; })
                .orElse(false);
    }

    @Transactional
    public boolean toggleImportant(Long subId, Long userId) {
        return subs.findById(subId)
                .filter(s -> s.getUser().getId().equals(userId))
                .map(s -> {
                    s.setImportant(!s.isImportant());
                    subs.save(s);
                    return true;
                })
                .orElse(false);
    }


    @Transactional(readOnly = true)
    public List<Subscription> listForUser(Long userId) {
        return subs.findAllByUser_Id(userId);
    }

    private static class ParsedInput {
        SubjectType type;
        String ruzKey;
        String name;
        String filtersJson;
    }
}
