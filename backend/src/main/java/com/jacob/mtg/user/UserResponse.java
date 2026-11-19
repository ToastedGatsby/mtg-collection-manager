package com.jacob.mtg.user;

/** What the API exposes about a user. Never return the entity itself (it holds the password hash). */
public record UserResponse(Long id, String email, String displayName, String role) {

    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getDisplayName(), user.getRole().name());
    }
}
