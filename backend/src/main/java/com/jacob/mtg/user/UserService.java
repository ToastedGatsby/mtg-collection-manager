package com.jacob.mtg.user;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.jacob.mtg.common.exception.NotFoundException;

@Service
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse getById(Long userId) {
        return userRepository.findByIdAndDeletedAtIsNull(userId)
            .map(UserResponse::from)
            .orElseThrow(() -> new NotFoundException("User not found"));
    }
}
