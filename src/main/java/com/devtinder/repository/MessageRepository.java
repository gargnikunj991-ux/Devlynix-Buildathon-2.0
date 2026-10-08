package com.devtinder.repository;

import com.devtinder.entity.Message;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByMatchIdOrderBySentAtAsc(Long matchId);

    List<Message> findByMatchIdAndIdGreaterThanOrderBySentAtAsc(Long matchId, Long afterId);

    long countByMatchIdAndSenderIdNotAndIsReadFalse(Long matchId, Long senderId);

    @Modifying
    @Query("""
            update Message m
            set m.isRead = true, m.readAt = :now
            where m.match.id = :matchId
              and m.sender.id <> :userId
              and m.isRead = false
            """)
    void markMessagesAsRead(@Param("matchId") Long matchId, @Param("userId") Long userId, @Param("now") Instant now);

    @Modifying
    @Query("delete from Message m where m.match.id = :matchId")
    void deleteByMatchId(@Param("matchId") Long matchId);
}

