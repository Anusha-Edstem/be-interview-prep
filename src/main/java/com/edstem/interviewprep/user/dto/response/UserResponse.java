package com.edstem.interviewprep.user.dto.response;

import com.edstem.interviewprep.user.entity.Role;
import java.time.Instant;
import java.util.UUID;

public record UserResponse(UUID id, String email, Role role, Instant createdDate) {}
