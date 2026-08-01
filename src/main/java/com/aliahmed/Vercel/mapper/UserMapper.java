package com.aliahmed.Vercel.mapper;

import com.aliahmed.Vercel.dto.CurrentUserResponse;
import com.aliahmed.Vercel.entity.User;
import org.springframework.stereotype.Component;

/**
 * Entity to DTO, in one place. Keeping this out of the entity is what stops a
 * column added tomorrow from silently appearing in the API response.
 */
@Component
public class UserMapper {

    public CurrentUserResponse toCurrentUserResponse(User user) {
        return new CurrentUserResponse(
                user.getId(),
                user.getGithubLogin(),
                user.getName(),
                user.getEmail(),
                user.getAvatarUrl());
    }
}
