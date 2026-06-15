package com.devtinder.service;

import com.devtinder.dto.response.MessageResponse;
import com.devtinder.entity.Match;
import com.devtinder.entity.Message;
import com.devtinder.entity.User;
import com.devtinder.exception.ResourceNotFoundException;
import com.devtinder.repository.MessageRepository;
import com.devtinder.repository.UserRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final MatchService matchService;

    public ChatService(
            MessageRepository messageRepository,
            UserRepository userRepository,
            MatchService matchService
    ) {
        this.messageRepository = messageRepository;
        this.userRepository = userRepository;
        this.matchService = matchService;
    }

    @Transactional(readOnly = true)
    public List<MessageResponse> getMessages(String email, Long matchId) {
        User user = findByEmail(email);
        matchService.getMatchForUser(matchId, user);

        return messageRepository.findByMatchIdOrderBySentAtAsc(matchId).stream()
                .map(MessageResponse::from)
                .toList();
    }

    @Transactional
    public MessageResponse sendMessage(String email, Long matchId, String content) {
        User sender = findByEmail(email);
        Match match = matchService.getMatchForUser(matchId, sender);

        Message saved = messageRepository.save(new Message(match, sender, content.trim()));
        return MessageResponse.from(saved);
    }

    private User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Profile not found"));
    }
}
