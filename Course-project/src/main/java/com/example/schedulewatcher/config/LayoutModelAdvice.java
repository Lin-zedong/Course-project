package com.example.schedulewatcher.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class LayoutModelAdvice {
    @ModelAttribute("uri")
    public String exposeRequestUri(HttpServletRequest req) {
        return (req != null && req.getRequestURI() != null) ? req.getRequestURI() : "";
    }
}
