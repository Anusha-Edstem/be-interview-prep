package com.edstem.interviewprep.auth.service;

import com.edstem.interviewprep.auth.dto.request.LoginRequest;
import com.edstem.interviewprep.auth.dto.request.RegisterRequest;
import com.edstem.interviewprep.auth.dto.response.AuthTokenResponse;
import com.edstem.interviewprep.auth.exception.EmailAlreadyRegisteredException;
import com.edstem.interviewprep.auth.exception.InvalidCredentialsException;
import com.edstem.interviewprep.user.dto.response.UserResponse;
import com.edstem.interviewprep.user.entity.Role;
import com.edstem.interviewprep.user.entity.User;
import com.edstem.interviewprep.user.mapper.UserMapper;
import com.edstem.interviewprep.user.repository.UserRepository;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtTokenService jwtTokenService;
  private final String absentUserHash;

  public AuthService(
      UserRepository userRepository,
      PasswordEncoder passwordEncoder,
      JwtTokenService jwtTokenService) {
    this.userRepository = userRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtTokenService = jwtTokenService;
    this.absentUserHash = passwordEncoder.encode(UUID.randomUUID().toString());
  }

  @Transactional
  public UserResponse register(RegisterRequest request) {
    String email = normalise(request.email());
    if (userRepository.existsByEmail(email)) {
      throw new EmailAlreadyRegisteredException();
    }
    User user = User.create(email, passwordEncoder.encode(request.password()), Role.USER);
    return UserMapper.toResponse(userRepository.save(user));
  }

  @Transactional(readOnly = true)
  public AuthTokenResponse login(LoginRequest request) {
    Optional<User> candidate = userRepository.findByEmail(normalise(request.email()));
    if (candidate.isEmpty()) {
      passwordEncoder.matches(request.password(), absentUserHash);
      throw new InvalidCredentialsException();
    }
    User user = candidate.get();
    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new InvalidCredentialsException();
    }
    return jwtTokenService.issueToken(user.getId(), user.getRole());
  }

  private String normalise(String email) {
    return email.trim().toLowerCase(Locale.ROOT);
  }
}
