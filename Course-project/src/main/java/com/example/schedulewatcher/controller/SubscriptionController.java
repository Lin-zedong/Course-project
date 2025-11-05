package com.example.schedulewatcher.controller;

import com.example.schedulewatcher.model.Subscription;
import com.example.schedulewatcher.model.User;
import com.example.schedulewatcher.service.SubscriptionService;
import com.example.schedulewatcher.service.UserSessionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
public class SubscriptionController {

    private final SubscriptionService svc;
    private final UserSessionService userSession;
    private static final ObjectMapper M = new ObjectMapper();

    public SubscriptionController(SubscriptionService svc, UserSessionService userSession) {
        this.svc = svc;
        this.userSession = userSession;
    }

    @GetMapping("/subscriptions")
    public String page(HttpSession session, Model model) {
        User me = userSession.current(session);
        model.addAttribute("email", me.getEmail());

        List<Subscription> list = svc.listForUser(me.getId());
        List<SubsView> vms = new ArrayList<>(list.size());
        for (Subscription s : list) {
            vms.add(new SubsView(
                    s.getId(),
                    s.getSubject().getName(),
                    s.getSubject().getRuzKey(),
                    s.isImportant(),
                    formatSession(s.getFilters())
            ));
        }
        model.addAttribute("subsView", vms);
        return "subscriptions";
    }

    @PostMapping(path = "/subscriptions", consumes = "application/x-www-form-urlencoded")
    public String add(HttpSession session,
                      @RequestParam Map<String, String> form,
                      RedirectAttributes ra) {
        User me = userSession.current(session);

        String code = firstNonBlank(
                form.get("code"), form.get("ruzKey"),
                form.get("subject"), form.get("input"), form.get("text"));

        if (code != null && !code.isBlank()) {
            try {
                Optional<?> created = svc.addByInput(me.getId(), code.trim());
                ra.addFlashAttribute("ok", created.isPresent() ? "Subscription added" : "Already subscribed");
            } catch (IllegalArgumentException ex) {
                ra.addFlashAttribute("err", ex.getMessage());
            } catch (Exception ex) {
                ra.addFlashAttribute("err", "Failed to add subscription");
            }
        } else {
            ra.addFlashAttribute("err", "Nothing submitted");
        }
        return "redirect:/subscriptions";
    }

    @PostMapping("/subscriptions/{id}/delete")
    public String delete(HttpSession session,
                         @PathVariable("id") Long id,
                         RedirectAttributes ra) {
        User me = userSession.current(session);
        boolean ok = svc.delete(id, me.getId());
        ra.addFlashAttribute(ok ? "ok" : "err", ok ? "Deleted" : "Not found");
        return "redirect:/subscriptions";
    }

    @PostMapping("/subscriptions/{id}/important")
    public String toggleImportant(HttpSession session,
                                  @PathVariable("id") Long id,
                                  RedirectAttributes ra) {
        User me = userSession.current(session);
        boolean ok = svc.toggleImportant(id, me.getId());
        ra.addFlashAttribute(ok ? "ok" : "err", ok ? "Updated" : "Not found");
        return "redirect:/subscriptions";
    }

    private static String firstNonBlank(String... vs) {
        if (vs == null) return null;
        for (String v : vs) if (v != null && !v.trim().isEmpty()) return v;
        return null;
    }

    private static String formatSession(String filtersJson) {
        if (filtersJson == null || filtersJson.isBlank()) return null;
        try {
            JsonNode n = M.readTree(filtersJson);
            String room = text(n, "room");
            String date = text(n, "date");
            String from = text(n, "from");
            String to   = text(n, "to");

            StringBuilder sb = new StringBuilder();
            if (!room.isEmpty()) sb.append(room);
            if (!room.isEmpty() && (!date.isEmpty() || !from.isEmpty() || !to.isEmpty())) sb.append(" · ");
            if (!date.isEmpty()) sb.append(date);
            if (!from.isEmpty() || !to.isEmpty()) {
                if (!date.isEmpty()) sb.append(" ");
                sb.append(from.isEmpty() ? "?" : from).append("–").append(to.isEmpty() ? "?" : to);
            }
            String str = sb.toString().trim();
            return str.isEmpty() ? null : str;
        } catch (Exception e) {
            return null;
        }
    }

    private static String text(JsonNode n, String k) {
        JsonNode v = n.path(k);
        return v.isMissingNode() || v.isNull() ? "" : v.asText("");
    }

    public static class SubsView {
        public Long id;
        public String subjectName;
        public String code;
        public boolean important;
        public String formattedSession;

        public SubsView(Long id, String subjectName, String code, boolean important, String formattedSession) {
            this.id = id;
            this.subjectName = subjectName;
            this.code = code;
            this.important = important;
            this.formattedSession = formattedSession;
        }

        public Long getId() { return id; }
        public String getSubjectName() { return subjectName; }
        public String getCode() { return code; }
        public boolean isImportant() { return important; }
        public String getFormattedSession() { return formattedSession; }
    }
}