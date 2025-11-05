package com.example.schedulewatcher.repository;
import com.example.schedulewatcher.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> { }
