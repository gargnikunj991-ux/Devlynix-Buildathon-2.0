package com.devtinder.repository;

import com.devtinder.entity.Message;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findByMatchIdOrderBySentAtAsc(Long matchId);
}
