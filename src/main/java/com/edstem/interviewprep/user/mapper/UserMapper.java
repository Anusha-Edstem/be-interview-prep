package com.edstem.interviewprep.user.mapper;

import com.edstem.interviewprep.user.dto.response.UserResponse;
import com.edstem.interviewprep.user.entity.User;

public final class UserMapper {

  private UserMapper() {}

  public static UserResponse toResponse(User user) {
    return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.getCreatedDate());
  }
}
