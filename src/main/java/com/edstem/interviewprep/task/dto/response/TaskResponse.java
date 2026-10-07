package com.edstem.interviewprep.task.dto.response;

import com.edstem.interviewprep.task.entity.TaskStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record TaskResponse(
    UUID id,
    String title,
    String description,
    TaskStatus status,
    LocalDate dueDate,
    Instant createdDate) {}
