package com.devtinder.repository;

import com.devtinder.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    @Query("""
            select distinct u
            from User u
            left join fetch u.skills
            where u.active = true
              and u.id <> :userId
              and u.id not in (
                select s.swiped.id
                from Swipe s
                where s.swiper.id = :userId
              )
            """)
    List<User> findDiscoverableUsers(@Param("userId") Long userId);
}
