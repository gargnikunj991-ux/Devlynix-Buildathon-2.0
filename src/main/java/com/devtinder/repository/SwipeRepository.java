package com.devtinder.repository;

import com.devtinder.entity.Swipe;
import com.devtinder.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SwipeRepository extends JpaRepository<Swipe, Long> {

    Optional<Swipe> findBySwiperIdAndSwipedId(Long swiperId, Long swipedId);

    @Query("""
            select s.swiper
            from Swipe s
            where s.swiped.id = :userId
              and s.direction = com.devtinder.entity.Swipe.Direction.LIKE
              and s.swiper.active = true
              and s.swiper.id not in (
                  select s2.swiped.id
                  from Swipe s2
                  where s2.swiper.id = :userId
              )
            order by s.createdAt desc
            """)
    List<User> findIncomingLikes(@Param("userId") Long userId);
}
