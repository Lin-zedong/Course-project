package com.example.schedulewatcher.service;

import com.example.schedulewatcher.model.AuditLog;
import com.example.schedulewatcher.model.Event;
import com.example.schedulewatcher.model.Subscription;
import com.example.schedulewatcher.model.User;
import com.example.schedulewatcher.repository.AuditLogRepository;
import com.example.schedulewatcher.repository.UserRepository;
import com.example.schedulewatcher.repository.SubjectRepository;
import com.example.schedulewatcher.model.Subject;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.hibernate.LazyInitializationException;

import java.util.List;

@Service
@Transactional
public class NotificationService {

    private final JavaMailSender mailSender;
    private final AuditLogRepository audit;
    private final UserRepository users;
    private final SubjectRepository subjects;

    public NotificationService(JavaMailSender mailSender,
                               AuditLogRepository audit,
                               UserRepository users,
                               SubjectRepository subjects) {
        this.mailSender = mailSender;
        this.audit = audit;
        this.users = users;
        this.subjects = subjects;
    }

    public void notifyByEmail(List<Subscription> subs, Event evt) {

        if (!isMeaningfulDiff(evt.getDiff())) { return; }

        for (Subscription s : subs) {
            String email = resolveEmail(s);
            if (email == null || email.isBlank()) continue;

            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setTo(email);

            String subjectName = resolveSubjectName(s, evt);
            msg.setSubject((s.isImportant() ? "[Important] " : "") + "[Schedule Change] " + subjectName);

            msg.setText("Schedule changed for " + subjectName + "\n" + formatDiffForEmail(evt.getDiff()));
            mailSender.send(msg);

            AuditLog log = new AuditLog();
            log.setAction("NOTIFY_SENT");
            log.setActor("system");
            log.setMeta("{\"channel\":\"email\",\"email\":\""+email+"\"}");
            audit.save(log);
        }
    }

    private String formatDiffForEmail(String diffJson) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            var n = om.readTree(diffJson);
            if (n.has("changes")) {
                var ch = n.get("changes");
                StringBuilder sb = new StringBuilder();
                var fields = ch.fieldNames();
                while (fields.hasNext()) {
                    var k = fields.next();
                    var item = ch.get(k);
                    String from = item.has("from") && !item.get("from").isNull() ? item.get("from").asText("") : "";
                    String to   = item.has("to")   && !item.get("to").isNull()   ? item.get("to").asText("")   : "";
                    if (!from.isEmpty() || !to.isEmpty()) {
                        sb.append(k).append(": ").append(from).append(" -> ").append(to).append("\n");
                    }
                }
                String s = sb.toString().trim();
                if (!s.isEmpty()) return s;
            }
            return diffJson;
        } catch (Exception e) {
            return diffJson;
        }
    }

    private String resolveEmail(Subscription s){
        try {
            // try lazy property first
            User u = s.getUser();
            if (u != null) return u.getEmail();
        } catch (LazyInitializationException ignore) {}

        try {
            Long uid = null;
            if (s.getUser() != null) uid = s.getUser().getId();
            if (uid != null) {
                return users.findById(uid).map(User::getEmail).orElse(null);
            }
        } catch (Exception ignore) {}
        return null;
    }

    private String resolveSubjectName(Subscription s, Event evt) {
        try {
            if (evt != null && evt.getSubject() != null && evt.getSubject().getName() != null) {
                return evt.getSubject().getName();
            }
        } catch (Exception ignore) {}

        Long sid = null;
        try {
            if (s.getSubject() != null) sid = s.getSubject().getId();
        } catch (Exception ignore) {}

        if (sid != null) {
            try {
                return subjects.findById(sid).map(Subject::getName).orElse("Subject#" + sid);
            } catch (Exception e) {
                return "Subject#" + sid;
            }
        }
        return "Subject";
    }

    public String formatDiffPlain(String diffJson) {
        if (diffJson == null || diffJson.isBlank()) return "";
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = om.readTree(diffJson);
            StringBuilder sb = new StringBuilder();
            if (root.has("type")) {
                String type = root.get("type").asText("");
                if (!type.isEmpty()) sb.append("Type: ").append(type).append("\n");
            }
            if (root.has("changes")) {
                com.fasterxml.jackson.databind.JsonNode ch = root.get("changes");
                if (ch.isObject()) {
                    var it = ch.fields();
                    while (it.hasNext()) {
                        var e = it.next();
                        String k = e.getKey();
                        var val = e.getValue();
                        String from = val.has("from") && !val.get("from").isNull() ? val.get("from").asText("") : "";
                        String to   = val.has("to")   && !val.get("to").isNull()   ? val.get("to").asText("")   : "";
                        if (!from.isEmpty() || !to.isEmpty()) {
                            sb.append(k).append(": ").append(from).append(" -> ").append(to).append("\n");
                        }
                    }
                }
            }
            String out = sb.toString().trim();
            return out.isEmpty() ? diffJson : out;
        } catch (Exception ex) {
            return diffJson;
        }
    }

    private boolean isMeaningfulDiff(String diffJson) {
        if (diffJson == null || diffJson.isBlank()) return false;
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode root = om.readTree(diffJson);
            if ("initial".equalsIgnoreCase(root.path("type").asText())) return false;

            com.fasterxml.jackson.databind.JsonNode ch = root.path("changes");
            if (ch.isMissingNode()) return false;
            if (ch.isObject() && ch.size() == 0) return false;
            if (ch.isTextual() && ch.asText().isBlank()) return false;

            if (root.has("changedFields") && root.get("changedFields").asInt(0) == 0) return false;
            return true;
        } catch (Exception ex) {
            return true;
        }
    }
}