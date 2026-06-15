package com.devtinder.service;

import com.devtinder.dto.response.MatchResponse;
import com.devtinder.dto.response.ProfileResponse;
import com.devtinder.entity.Match;
import com.devtinder.entity.Swipe;
import com.devtinder.entity.User;
import com.devtinder.exception.ResourceNotFoundException;
import com.devtinder.repository.MatchRepository;
import com.devtinder.repository.SwipeRepository;
import com.devtinder.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class MatchService {

    private final MatchRepository matchRepository;
    private final SwipeRepository swipeRepository;
    private final UserRepository userRepository;

    public MatchService(
            MatchRepository matchRepository,
            SwipeRepository swipeRepository,
            UserRepository userRepository
    ) {
        this.matchRepository = matchRepository;
        this.swipeRepository = swipeRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public MatchResponse swipe(User swiper, Long targetUserId, Swipe.Direction direction) {
        if (swiper.getId().equals(targetUserId)) {
            throw new IllegalArgumentException("You cannot swipe on yourself");
        }

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User to swipe was not found"));

        Swipe swipe = swipeRepository.findBySwiperIdAndSwipedId(swiper.getId(), target.getId())
                .orElseGet(() -> new Swipe(swiper, target, direction));
        swipe.setDirection(direction);
        swipeRepository.save(swipe);

        if (direction != Swipe.Direction.LIKE) {
            return new MatchResponse(null, ProfileResponse.from(target), null, false);
        }

        boolean targetLikedBack = swipeRepository.findBySwiperIdAndSwipedId(target.getId(), swiper.getId())
                .map(existingSwipe -> existingSwipe.getDirection() == Swipe.Direction.LIKE)
                .orElse(false);

        if (!targetLikedBack) {
            return new MatchResponse(null, ProfileResponse.from(target), null, false);
        }

        Match match = matchRepository.findBetweenUsers(swiper.getId(), target.getId())
                .orElseGet(() -> matchRepository.save(new Match(swiper, target)));

        return toResponse(match, swiper);
    }

    @Transactional(readOnly = true)
    public List<MatchResponse> getMatches(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));

        return matchRepository.findByUserOneIdOrUserTwoIdOrderByCreatedAtDesc(user.getId(), user.getId()).stream()
                .map(match -> toResponse(match, user))
                .toList();
    }

    Match getMatchForUser(Long matchId, User user) {
        Match match = matchRepository.findById(matchId)
                .orElseThrow(() -> new ResourceNotFoundException("Match not found"));

        if (!belongsToMatch(match, user)) {
            throw new ResourceNotFoundException("Match not found");
        }

        return match;
    }

    private MatchResponse toResponse(Match match, User viewer) {
        User otherUser = match.getUserOne().getId().equals(viewer.getId())
                ? match.getUserTwo()
                : match.getUserOne();

        return new MatchResponse(match.getId(), ProfileResponse.from(otherUser), match.getCreatedAt(), true);
    }

    private static boolean belongsToMatch(Match match, User user) {
        return match.getUserOne().getId().equals(user.getId()) || match.getUserTwo().getId().equals(user.getId());
    }
}
