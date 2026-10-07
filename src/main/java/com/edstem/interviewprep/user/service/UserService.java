package com.edstem.interviewprep.user.service;

import com.edstem.interviewprep.common.dto.response.PageResponse;
import com.edstem.interviewprep.user.dto.response.UserResponse;
import com.edstem.interviewprep.user.exception.UserNotFoundException;
import com.edstem.interviewprep.user.mapper.UserMapper;
import com.edstem.interviewprep.user.repository.UserRepository;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

  private final UserRepository userRepository;

  public UserService(UserRepository userRepository) {
    this.userRepository = userRepository;
  }

  @Transactional(readOnly = true)
  public UserResponse getProfile(UUID id) {
    return userRepository
        .findById(id)
        .map(UserMapper::toResponse)
        .orElseThrow(() -> new UserNotFoundException(id));
  }

  @Transactional(readOnly = true)
  public PageResponse<UserResponse> listUsers(Pageable pageable) {
    return PageResponse.from(userRepository.findAll(pageable).map(UserMapper::toResponse));
  }
}
