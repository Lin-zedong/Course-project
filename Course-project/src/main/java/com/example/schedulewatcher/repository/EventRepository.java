package com.example.schedulewatcher.repository;
import com.example.schedulewatcher.model.Event;
import com.example.schedulewatcher.model.Subject;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.List;
public interface EventRepository extends JpaRepository<Event, Long> {
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query(value = """
    INSERT INTO events (created_at, diff, event_time, hash, subject_id)
    VALUES (:createdAt, CAST(:diffJson AS jsonb), :eventTime, :hash, :subjectId)
    ON CONFLICT (subject_id, hash) DO NOTHING
    """, nativeQuery = true)
    int insertIgnore(@Param("createdAt") java.time.OffsetDateTime createdAt,
                     @Param("diffJson") String diffJson,
                     @Param("eventTime") java.time.OffsetDateTime eventTime,
                     @Param("hash") String hash,
                     @Param("subjectId") Long subjectId);

    default int insertIgnoreFromEntity(Event e) {
        return insertIgnore(e.getCreatedAt() != null ? e.getCreatedAt() : java.time.OffsetDateTime.now(),
                e.getDiff(), e.getEventTime(), e.getHash(), e.getSubject().getId());
    }

    List<Event> findBySubjectInOrderByEventTimeDesc(List<Subject> subjects);
    long countByEventTimeAfter(OffsetDateTime since);
}
