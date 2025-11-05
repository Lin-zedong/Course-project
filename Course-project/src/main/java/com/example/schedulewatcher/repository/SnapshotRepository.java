package com.example.schedulewatcher.repository;
import com.example.schedulewatcher.model.Snapshot;
import com.example.schedulewatcher.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface SnapshotRepository extends JpaRepository<Snapshot, Long> {
    Optional<Snapshot> findTopBySubjectOrderBySnapshotAtDesc(Subject subject);
}
