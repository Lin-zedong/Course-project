package com.example.schedulewatcher.repository;

import com.example.schedulewatcher.model.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {

    List<Subscription> findAllByUser_Id(Long userId);

    Optional<Subscription> findByUser_IdAndSubject_Id(Long userId, Long subjectId);

    Optional<Subscription> findByIdAndUser_Id(Long id, Long userId);

    boolean existsByUser_IdAndSubject_Id(Long userId, Long subjectId);

    @Query("select s from Subscription s join fetch s.user where s.subject.id = :subjectId")
    List<Subscription> findAllBySubjectIdFetchUser(@Param("subjectId") Long subjectId);

    @Query("select s from Subscription s join fetch s.user")
    List<Subscription> findAllWithUser();

}
