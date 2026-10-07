package com.edstem.interviewprep.task.dto.request;

import com.edstem.interviewprep.task.entity.TaskStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record UpdateTaskRequest(
    @NotBlank(message = "title is required")
        @Size(max = 100, message = "title must be at most 100 characters")
        String title,
    @Size(max = 2000, message = "description must be at most 2000 characters") String description,
    @NotNull(message = "status is required") TaskStatus status,
    @FutureOrPresent(message = "dueDate must not be in the past") LocalDate dueDate) {}
