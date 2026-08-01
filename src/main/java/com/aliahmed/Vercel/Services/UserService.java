package com.aliahmed.Vercel.Services;

import com.aliahmed.Vercel.Repositories.UserRepository;
import com.aliahmed.Vercel.dto.GithubUserResponse;
import com.aliahmed.Vercel.entity.User;
import com.aliahmed.Vercel.exception.ResourceNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Matches on GitHub's numeric id rather than the login, because a user can
     * rename their GitHub account at any time and the numeric id cannot change.
     * An existing user's profile is refreshed on every login.
     */
    @Transactional
    public User findOrCreateFromGithub(GithubUserResponse profile, String email) {
        User user = userRepository.findByGithubId(profile.id())
                .orElseGet(() -> User.builder().githubId(profile.id()).build());

        user.setGithubLogin(profile.login());
        user.setName(profile.name());
        user.setAvatarUrl(profile.avatarUrl());
        if (email != null) {
            user.setEmail(email);
        }
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User getById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("No user with id " + id));
    }
}
