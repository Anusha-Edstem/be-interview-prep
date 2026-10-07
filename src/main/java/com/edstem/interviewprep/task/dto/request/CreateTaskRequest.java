package com.edstem.interviewprep.task.dto.request;

import com.edstem.interviewprep.task.entity.TaskStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CreateTaskRequest(
    @NotBlank(message = "title is required")
        @Size(max = 100, message = "title must be at most 100 characters")
        String title,
    @Size(max = 2000, message = "description must be at most 2000 characters") String description,
    TaskStatus status,
    @FutureOrPresent(message = "dueDate must not be in the past") LocalDate dueDate) {

  public TaskStatus statusOrDefault() {
    return status == null ? TaskStatus.TODO : status;
  }
}
