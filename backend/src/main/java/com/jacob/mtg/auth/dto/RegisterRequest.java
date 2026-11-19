package com.jacob.mtg.auth.dto;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank @Size(min = 2, max = 50) String displayName,
    @NotBlank @Email @Size(max = 254) String email,
    // Length over complexity rules (NIST 800-63B). BCrypt only uses the first 72 bytes - checked in AuthService.
    @NotBlank @Size(min = 12, max = 72) String password,
    // GDPR: consent must be an explicit action (unchecked checkbox in the UI)
    @AssertTrue(message = "You must accept the Terms of Service and the Privacy Policy") boolean acceptTerms) {
}
