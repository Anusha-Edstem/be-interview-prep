package com.edstem.interviewprep.user.exception;

import com.edstem.interviewprep.common.exception.ApiException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class UserNotFoundException extends ApiException {

  public UserNotFoundException(UUID id) {
    super(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "No user exists with id " + id);
  }
}
