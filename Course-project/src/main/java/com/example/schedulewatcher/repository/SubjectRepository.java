package com.example.schedulewatcher.repository;

import com.example.schedulewatcher.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SubjectRepository extends JpaRepository<Subject, Long> {
    Optional<Subject> findByRuzKey(String ruzKey);
}
