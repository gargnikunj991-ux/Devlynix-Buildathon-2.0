package com.devtinder.repository;

import com.devtinder.entity.Match;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MatchRepository extends JpaRepository<Match, Long> {

    @Query("""
            select m
            from Match m
            where (m.userOne.id = :firstUserId and m.userTwo.id = :secondUserId)
               or (m.userOne.id = :secondUserId and m.userTwo.id = :firstUserId)
            """)
    Optional<Match> findBetweenUsers(
            @Param("firstUserId") Long firstUserId,
            @Param("secondUserId") Long secondUserId
    );

    List<Match> findByUserOneIdOrUserTwoIdOrderByCreatedAtDesc(Long userOneId, Long userTwoId);
}
