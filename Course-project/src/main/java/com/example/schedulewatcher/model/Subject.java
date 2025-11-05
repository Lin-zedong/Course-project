package com.example.schedulewatcher.model;

import jakarta.persistence.*;

@Entity
@Table(name = "subjects")
public class Subject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(name = "ruz_key", nullable = false, unique = true)
    private String ruzKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", columnDefinition = "subject_type", nullable = false)
    private SubjectType type;

    // getters / setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRuzKey() { return ruzKey; }
    public void setRuzKey(String ruzKey) { this.ruzKey = ruzKey; }

    public SubjectType getType() { return type; }
    public void setType(SubjectType type) { this.type = type; }
}
