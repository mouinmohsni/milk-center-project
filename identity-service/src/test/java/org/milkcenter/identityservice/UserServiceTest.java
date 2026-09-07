package org.milkcenter.identityservice;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.milkcenter.identityservice.dto.request.UserRegisterRequest;
import org.milkcenter.identityservice.enums.Role;
import org.milkcenter.identityservice.model.User;
import org.milkcenter.identityservice.repository.UserRepository;
import org.milkcenter.identityservice.service.UserService;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void registerCreatesFarmerAndEncodesPassword() {
        UserRegisterRequest request = UserRegisterRequest.builder()
                .username("farmer01")
                .password("password123")
                .firstName("Ali")
                .lastName("Farmer")
                .phoneNumber("20112233")
                .build();

        when(userRepository.existsByUsername("farmer01"))
                .thenReturn(false);
        when(userRepository.existsByPhoneNumber("20112233"))
                .thenReturn(false);
        when(passwordEncoder.encode("password123"))
                .thenReturn("encoded-password");
        when(userRepository.save(any(User.class)))
                .thenAnswer(invocation -> {
                    User user = invocation.getArgument(0);
                    user.setId(1L);
                    return user;
                });

        var response = userService.registerUser(request);

        assertEquals(1L, response.getId());
        assertEquals(Role.FARMER, response.getRole());
        assertTrue(response.isEnabled());
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void registerRejectsDuplicateUsername() {
        UserRegisterRequest request = UserRegisterRequest.builder()
                .username("farmer01")
                .password("password123")
                .firstName("Ali")
                .lastName("Farmer")
                .build();

        when(userRepository.existsByUsername("farmer01"))
                .thenReturn(true);

        assertThrows(
                ResponseStatusException.class,
                () -> userService.registerUser(request)
        );

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void disabledUserCannotLogin() {
        User user = User.builder()
                .username("farmer01")
                .password("encoded-password")
                .role(Role.FARMER)
                .isEnabled(false)
                .build();

        when(userRepository.findByUsername("farmer01"))
                .thenReturn(Optional.of(user));

        assertFalse(
                userService.verifyLogin("farmer01", "password123")
        );
    }

    @Test
    void validUserCanLogin() {
        User user = User.builder()
                .username("farmer01")
                .password("encoded-password")
                .role(Role.FARMER)
                .isEnabled(true)
                .build();

        when(userRepository.findByUsername("farmer01"))
                .thenReturn(Optional.of(user));
        when(passwordEncoder.matches(
                "password123",
                "encoded-password"
        )).thenReturn(true);

        assertTrue(
                userService.verifyLogin("farmer01", "password123")
        );
    }
}
