package com.edstem.interviewprep.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edstem.interviewprep.auth.dto.request.LoginRequest;
import com.edstem.interviewprep.auth.dto.request.RegisterRequest;
import com.edstem.interviewprep.auth.dto.response.AuthTokenResponse;
import com.edstem.interviewprep.auth.exception.EmailAlreadyRegisteredException;
import com.edstem.interviewprep.auth.exception.InvalidCredentialsException;
import com.edstem.interviewprep.user.dto.response.UserResponse;
import com.edstem.interviewprep.user.entity.Role;
import com.edstem.interviewprep.user.entity.User;
import com.edstem.interviewprep.user.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AuthServiceTest {

  private static final String PASSWORD = "correct-horse-battery";

  @Mock private UserRepository userRepository;

  @Mock private JwtTokenService jwtTokenService;

  private PasswordEncoder passwordEncoder;
  private AuthService authService;

  @BeforeEach
  void setUp() {
    passwordEncoder = new BCryptPasswordEncoder(4);
    authService = new AuthService(userRepository, passwordEncoder, jwtTokenService);
  }

  @Test
  void registerStoresAHashRatherThanThePlaintextPassword() {
    when(userRepository.existsByEmail("ada@example.com")).thenReturn(false);
    when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

    UserResponse response = authService.register(new RegisterRequest("ada@example.com", PASSWORD));

    ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
    verify(userRepository).save(saved.capture());
    assertThat(saved.getValue().getPasswordHash()).isNotEqualTo(PASSWORD);
    assertThat(saved.getValue().getPasswordHash()).doesNotContain(PASSWORD);
    assertThat(passwordEncoder.matches(PASSWORD, saved.getValue().getPasswordHash())).isTrue();
    assertThat(response.email()).isEqualTo("ada@example.com");
  }

  @Test
  void registerGivesEveryNewAccountTheUserRole() {
    when(userRepository.existsByEmail("ada@example.com")).thenReturn(false);
    when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

    UserResponse response = authService.register(new RegisterRequest("ada@example.com", PASSWORD));

    assertThat(response.role()).isEqualTo(Role.USER);
  }

  @Test
  void registerNormalisesTheEmailBeforeStoringIt() {
    when(userRepository.existsByEmail("ada@example.com")).thenReturn(false);
    when(userRepository.save(any(User.class))).thenAnswer(call -> call.getArgument(0));

    UserResponse response =
        authService.register(new RegisterRequest("  Ada@Example.COM  ", PASSWORD));

    assertThat(response.email()).isEqualTo("ada@example.com");
  }

  @Test
  void registerRejectsAnEmailThatIsAlreadyTaken() {
    when(userRepository.existsByEmail("ada@example.com")).thenReturn(true);

    assertThatThrownBy(() -> authService.register(new RegisterRequest("ada@example.com", PASSWORD)))
        .isInstanceOf(EmailAlreadyRegisteredException.class);
    verify(userRepository, never()).save(any(User.class));
  }

  @Test
  void loginIssuesATokenWhenTheCredentialsMatch() {
    User user = User.create("ada@example.com", passwordEncoder.encode(PASSWORD), Role.USER);
    when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));
    AuthTokenResponse issued =
        new AuthTokenResponse("a.token.value", "Bearer", 900L, Instant.now().plusSeconds(900));
    when(jwtTokenService.issueToken(user.getId(), user.getRole())).thenReturn(issued);

    AuthTokenResponse response = authService.login(new LoginRequest("ada@example.com", PASSWORD));

    assertThat(response.accessToken()).isEqualTo("a.token.value");
    assertThat(response.expiresInSeconds()).isEqualTo(900L);
  }

  @Test
  void loginRejectsAWrongPasswordWithoutIssuingAToken() {
    User user = User.create("ada@example.com", passwordEncoder.encode(PASSWORD), Role.USER);
    when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));

    assertThatThrownBy(
            () -> authService.login(new LoginRequest("ada@example.com", "not-the-password")))
        .isInstanceOf(InvalidCredentialsException.class);
    verify(jwtTokenService, never()).issueToken(any(UUID.class), any(Role.class));
  }

  @Test
  void loginRejectsAnUnknownEmailWithTheSameMessageAsAWrongPassword() {
    when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());
    User user = User.create("ada@example.com", passwordEncoder.encode(PASSWORD), Role.USER);
    when(userRepository.findByEmail("ada@example.com")).thenReturn(Optional.of(user));

    String unknownEmailMessage =
        catchMessage(() -> authService.login(new LoginRequest("nobody@example.com", PASSWORD)));
    String wrongPasswordMessage =
        catchMessage(() -> authService.login(new LoginRequest("ada@example.com", "wrong-one")));

    assertThat(unknownEmailMessage).isEqualTo(wrongPasswordMessage);
  }

  private String catchMessage(Runnable call) {
    try {
      call.run();
      throw new AssertionError("expected the login to be rejected");
    } catch (InvalidCredentialsException rejected) {
      return rejected.getMessage();
    }
  }
}
