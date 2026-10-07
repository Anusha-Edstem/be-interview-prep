package com.edstem.interviewprep.common.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
    Instant timestamp,
    int status,
    String code,
    String message,
    String path,
    List<FieldErrorDetail> fieldErrors) {

  public static ErrorResponse of(int status, String code, String message, String path) {
    return new ErrorResponse(Instant.now(), status, code, message, path, List.of());
  }

  public static ErrorResponse of(
      int status, String code, String message, String path, List<FieldErrorDetail> fieldErrors) {
    return new ErrorResponse(Instant.now(), status, code, message, path, fieldErrors);
  }
}
