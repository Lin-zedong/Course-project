package com.example.schedulewatcher.controller;

import com.example.schedulewatcher.model.Event;
import com.example.schedulewatcher.model.Subscription;
import com.example.schedulewatcher.repository.EventRepository;
import com.example.schedulewatcher.repository.SubscriptionRepository;
import com.example.schedulewatcher.service.NotificationService;
import com.example.schedulewatcher.util.ScheduleDiffUtil;

import com.example.schedulewatcher.repository.SubjectRepository;
import com.example.schedulewatcher.model.Subject;
import com.example.schedulewatcher.model.SubjectType;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/admin/subscriptions")
public class AdminSubscriptionController {

    private final SubscriptionRepository subs;
    private final EventRepository events;
    private final NotificationService notifier;

    @org.springframework.beans.factory.annotation.Autowired
    private SubjectRepository subjects;

    private static final ObjectMapper M = new ObjectMapper();

    public AdminSubscriptionController(SubscriptionRepository subs,
                                       EventRepository events,
                                       NotificationService notifier) {
        this.subs = subs;
        this.events = events;
        this.notifier = notifier;
    }

    private String normalizeTeacher(String t) {
        if (t == null) return "";
        t = t.trim();
        if (t.toLowerCase().startsWith("t-")) return t.substring(2);
        return t;
    }

    @GetMapping
    public String index(Model model) {
        List<Subscription> all = subs.findAll();
        model.addAttribute("subs", all);
        return "redirect:/admin/manage";
    }

    @GetMapping("/{id}/edit")
    public String edit(@PathVariable Long id, Model model) {
        Subscription s = subs.findById(id).orElseThrow();
        Map<String,String> f = parseFilters(s.getFilters());
        model.addAttribute("s", s);
        model.addAttribute("room", f.getOrDefault("room",""));
        model.addAttribute("date", f.getOrDefault("date",""));
        model.addAttribute("from", f.getOrDefault("from",""));
        model.addAttribute("to",   f.getOrDefault("to",""));

        String _t = f.getOrDefault("teacher", "");
        if ((_t == null || _t.isBlank())
                && s.getSubject() != null
                && s.getSubject().getType() == SubjectType.TEACHER) {
            _t = s.getSubject().getRuzKey();
        }
        model.addAttribute("teacher", _t);

        return "admin/subscription_edit";
    }

    @PostMapping("/{id}/edit")
    @Transactional
    public String save(@PathVariable Long id,
                       org.springframework.web.servlet.mvc.support.RedirectAttributes ra,
                       @RequestParam(required=false) String room,
                       @RequestParam(required=false) String date,
                       @RequestParam(required=false, name="from") String fromTime,
                       @RequestParam(required=false, name="to") String toTime,
                       @RequestParam(required=false) String teacher) {
        Subscription s = subs.findById(id).orElseThrow();

        final String normalizedTeacher = normalizeTeacher(teacher);

        try {
            if (s.getSubject() != null && s.getSubject().getType() == SubjectType.TEACHER) {
                String currentKey = s.getSubject().getRuzKey();
                if (normalizedTeacher != null
                        && !normalizedTeacher.isBlank()
                        && !java.util.Objects.equals(currentKey, normalizedTeacher)) {

                    Subject newSubject = subjects.findByRuzKey(normalizedTeacher).orElse(null);
                    if (newSubject == null) {
                        Subject ns = new Subject();
                        ns.setName("Teacher " + normalizedTeacher);
                        ns.setRuzKey(normalizedTeacher);
                        ns.setType(SubjectType.TEACHER);
                        newSubject = subjects.save(ns);
                    }
                    s.setSubject(newSubject);
                }
            }
        } catch (Exception ignore) {}

        String oldFilters = s.getFilters();

        String newFilters = buildFiltersJson(room, date, fromTime, toTime, normalizedTeacher);

        s.setFilters(newFilters);
        subs.save(s);

        String diffJson = buildManualDiff(oldFilters, newFilters);

        Event evt = new Event();
        evt.setSubject(s.getSubject());
        evt.setEventTime(OffsetDateTime.now());
        evt.setCreatedAt(OffsetDateTime.now());
        evt.setDiff(diffJson);
        evt.setHash(ScheduleDiffUtil.sha256(s.getSubject().getId() + ":" + diffJson));
        events.insertIgnoreFromEntity(evt);

        notifier.notifyByEmail(java.util.List.of(s), evt);

        if (ra != null) ra.addFlashAttribute("flashKey", "flash.subscription_saved");
        return "redirect:/admin/manage";
    }

    private static Map<String, String> parseFilters(String json) {
        Map<String,String> out = new HashMap<>();
        if (json == null || json.isBlank()) return out;
        try {
            var n = M.readTree(json);
            for (String k : new String[]{"room","date","from","to","teacher"}) {
                if (n.has(k) && !n.get(k).isNull()) out.put(k, n.get(k).asText(""));
            }
        } catch (Exception ignore) {}
        return out;
    }

    private static String buildFiltersJson(String room, String date, String from, String to, String teacher) {
        ObjectNode n = M.createObjectNode();
        if (room != null && !room.isBlank()) n.put("room", room.trim());
        if (date != null && !date.isBlank()) n.put("date", date.trim());
        if (from != null && !from.isBlank()) n.put("from", from.trim());
        if (to != null && !to.isBlank()) n.put("to", to.trim());
        if (teacher != null && !teacher.isBlank()) n.put("teacher", teacher.trim());
        return n.toString();
    }

    private static String buildManualDiff(String oldJson, String newJson) {
        try {
            Map<String,String> o = parseFilters(oldJson);
            Map<String,String> n = parseFilters(newJson);
            ObjectNode root = M.createObjectNode();
            root.put("type","manual");
            ObjectNode ch = root.putObject("changes");
            for (String k : new String[]{"room","date","from","to","teacher"}) {
                String ov = o.getOrDefault(k, "");
                String nv = n.getOrDefault(k, "");
                if (!java.util.Objects.equals(ov, nv)) {
                    ObjectNode item = ch.putObject(k);
                    item.put("from", ov);
                    item.put("to", nv);
                }
            }
            return root.toString();
        } catch (Exception e) {
            return ScheduleDiffUtil.diff(oldJson, newJson);
        }
    }
}