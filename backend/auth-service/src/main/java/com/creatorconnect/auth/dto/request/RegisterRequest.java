package com.creatorconnect.auth.dto.request;

import com.creatorconnect.auth.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Request payload for {@code POST /auth/register}.
 *
 * <p>Carries the personal data required to open a new CreatorConnect account.
 * Every field is validated by Jakarta Bean Validation before the controller
 * forwards the request to the service layer:
 * <ul>
 *   <li>Names must be non-blank and fit the column size (50 chars).</li>
 *   <li>Email must be a well-formed address (max 150 chars).</li>
 *   <li>Password must be 8&ndash;64 chars and contain at least one uppercase
 *       letter, one lowercase letter, one digit, and one special character.</li>
 *   <li>Phone is optional but limited to 20 chars.</li>
 *   <li>Role must be a valid {@link Role} value.</li>
 * </ul>
 *
 * <p>Validation failures are translated into {@code 400 BAD_REQUEST} responses
 * by {@code GlobalExceptionHandler}.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "First name is required")
    @Size(max = 50, message = "First name must not exceed 50 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 50, message = "Last name must not exceed 50 characters")
    private String lastName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a well-formed email address")
    @Size(max = 150, message = "Email must not exceed 150 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 8, max = 64, message = "Password must be between 8 and 64 characters")
    @Pattern(
            regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,64}$",
            message = "Password must contain at least one uppercase letter, one lowercase letter, "
                    + "one digit, and one special character"
    )
    private String password;

    @Size(max = 20, message = "Phone must not exceed 20 characters")
    private String phone;

    @NotNull(message = "Role is required")
    private Role role;
}
