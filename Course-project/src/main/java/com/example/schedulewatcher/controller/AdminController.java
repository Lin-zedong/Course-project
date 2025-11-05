package com.example.schedulewatcher.controller;

import com.example.schedulewatcher.repository.EventRepository;
import com.example.schedulewatcher.repository.SubscriptionRepository;
import com.example.schedulewatcher.repository.UserRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.OffsetDateTime;

@Controller
@RequestMapping("/admin")
public class AdminController {
    private final UserRepository users;
    private final SubscriptionRepository subs;
    private final EventRepository events;

    public AdminController(UserRepository users, SubscriptionRepository subs, EventRepository events) {
        this.users = users;
        this.subs = subs;
        this.events = events;
    }

    
    @GetMapping({"", "/", "/manage"})
    public String manage(Model model) {
        model.addAttribute("userCount", users.count());
        model.addAttribute("subscriptionCount", subs.count());
        model.addAttribute("eventsToday", events.countByEventTimeAfter(OffsetDateTime.now().minusDays(1)));

        var all = subs.findAll();
        java.util.List<Row> courses = new java.util.ArrayList<>();
        java.util.List<Row> teachers = new java.util.ArrayList<>();
        for (var s : all) {
            String email = s.getUser() != null ? s.getUser().getEmail() : "";
            String subjectName = s.getSubject() != null ? s.getSubject().getName() : "";
            String code = s.getSubject() != null ? s.getSubject().getRuzKey() : "";
            String pretty = prettyFilters(s.getFilters());
            Row r = new Row(s.getId(), email, subjectName, code, pretty, s.getSubject()!=null ? s.getSubject().getType() : null);
            if (r.type != null && r.type.name().equals("TEACHER")) teachers.add(r); else courses.add(r);
        }
        model.addAttribute("courseRows", courses);
        model.addAttribute("teacherRows", teachers);
        return "admin/manage";
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model) {
        return "redirect:/admin/manage";
    }

    private String prettyFilters(String json) {
        if (json == null || json.isBlank()) return "";
        try {
            var M = new com.fasterxml.jackson.databind.ObjectMapper();
            var n = M.readTree(json);
            String room = n.hasNonNull("room") ? n.get("room").asText("") : "";
            String date = n.hasNonNull("date") ? n.get("date").asText("") : "";
            String from = n.hasNonNull("from") ? n.get("from").asText("") : "";
            String to   = n.hasNonNull("to")   ? n.get("to").asText("")   : "";
            String teacher = n.hasNonNull("teacher") ? n.get("teacher").asText("") : "";
            StringBuilder sb = new StringBuilder();
            if (!room.isEmpty()) sb.append(room);
            if ((!room.isEmpty()) && (!date.isEmpty() || !from.isEmpty() || !to.isEmpty())) sb.append(" · ");
            if (!date.isEmpty()) sb.append(date);
            String time = (!from.isEmpty() || !to.isEmpty()) ? (from.isEmpty() ? "?" : from) + "–" + (to.isEmpty() ? "?" : to) : "";
            if (!time.isEmpty()) sb.append(sb.length()>0 ? " " : "").append(time);
            if (!teacher.isEmpty()) sb.append(sb.length()>0 ? " · " : "").append(teacher);
            return sb.toString();
        } catch (Exception e) {
            return json;
        }
    }

    public static class Row {
        public Long id;
        public String userEmail;
        public String subjectName;
        public String code;
        public String pretty;
        public com.example.schedulewatcher.model.SubjectType type;
        public Row(Long id, String userEmail, String subjectName, String code, String pretty, com.example.schedulewatcher.model.SubjectType type) {
            this.id = id; this.userEmail = userEmail; this.subjectName = subjectName; this.code = code; this.pretty = pretty; this.type = type;
        }
        public Long getId(){return id;}
        public String getUserEmail(){return userEmail;}
        public String getSubjectName(){return subjectName;}
        public String getCode(){return code;}
        public String getPretty(){return pretty;}
        public com.example.schedulewatcher.model.SubjectType getType(){return type;}
    }

}
