package com.example.schedulewatcher.controller;

import com.example.schedulewatcher.model.User;
import com.example.schedulewatcher.service.UserSessionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class WebController {

    private final UserSessionService userSession;

    public WebController(UserSessionService userSession) {
        this.userSession = userSession;
    }

    @GetMapping("/")
    public String index(HttpSession session, Model model) {
        User me = userSession.current(session);
        model.addAttribute("email", me.getEmail());
        return "index";
    }

    @PostMapping(path = "/profile/email", consumes = "application/x-www-form-urlencoded")
    public String saveEmail(HttpSession session,
                            @RequestParam("email") String email,
                            RedirectAttributes ra) {
        userSession.updateEmail(session, email);
        ra.addFlashAttribute("ok", "Saved");
        return "redirect:/subscriptions";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }
}
