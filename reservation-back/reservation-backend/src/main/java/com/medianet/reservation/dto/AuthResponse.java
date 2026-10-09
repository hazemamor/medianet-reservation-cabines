package com.medianet.reservation.dto;

public record AuthResponse(
        String token,
        String type,
        Long userId,
        String email,
        String role
) {
    public static AuthResponse of(String token, Long userId, String email, String role) {
        return new AuthResponse(token, "Bearer", userId, email, role);
    }
}
