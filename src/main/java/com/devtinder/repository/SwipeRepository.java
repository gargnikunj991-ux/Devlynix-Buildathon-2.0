package com.devtinder.repository;

import com.devtinder.entity.Swipe;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SwipeRepository extends JpaRepository<Swipe, Long> {

    Optional<Swipe> findBySwiperIdAndSwipedId(Long swiperId, Long swipedId);
}
