package com.example.schedulewatcher.controller.dto;

import java.util.UUID;

public class SubscriptionView {
    private Long id;
    private String subjectName;
    private boolean important;
    private String formattedSession; // 可为空

    public SubscriptionView(Long id, String subjectName, boolean important, String formattedSession) {
        this.id = id;
        this.subjectName = subjectName;
        this.important = important;
        this.formattedSession = formattedSession;
    }

    public Long getId() { return id; }
    public String getSubjectName() { return subjectName; }
    public boolean isImportant() { return important; }
    public String getFormattedSession() { return formattedSession; }
}
