package com.highlight.highlight_backend.repository.user;

import com.highlight.highlight_backend.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    boolean existsByUserId(String userId);
    boolean existsByNickname(String nickname);
    boolean existsByPhoneNumber(String phoneNumber);

    Optional<User> findByUserId(String userId);

    Optional<User> findByPhoneNumber(String phoneNumber);
}
