package com.edstem.interviewprep.task.exception;

import com.edstem.interviewprep.common.exception.ApiException;
import java.util.UUID;
import org.springframework.http.HttpStatus;

public class TaskNotFoundException extends ApiException {

  public TaskNotFoundException(UUID id) {
    super(HttpStatus.NOT_FOUND, "TASK_NOT_FOUND", "No task exists with id " + id);
  }
}
