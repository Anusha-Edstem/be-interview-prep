package com.edstem.interviewprep.task.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.edstem.interviewprep.common.dto.response.PageResponse;
import com.edstem.interviewprep.common.security.JsonAccessDeniedHandler;
import com.edstem.interviewprep.common.security.JsonAuthenticationEntryPoint;
import com.edstem.interviewprep.common.security.SecurityConfig;
import com.edstem.interviewprep.task.dto.request.CreateTaskRequest;
import com.edstem.interviewprep.task.dto.response.TaskResponse;
import com.edstem.interviewprep.task.entity.TaskStatus;
import com.edstem.interviewprep.task.exception.TaskNotFoundException;
import com.edstem.interviewprep.task.service.TaskService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@Import({SecurityConfig.class, JsonAuthenticationEntryPoint.class, JsonAccessDeniedHandler.class})
@WebMvcTest(TaskController.class)
class TaskControllerTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private TaskService taskService;

  private TaskResponse sampleResponse(UUID id) {
    return new TaskResponse(
        id,
        "Write report",
        "Quarterly",
        TaskStatus.TODO,
        LocalDate.now().plusDays(3),
        Instant.parse("2026-01-01T00:00:00Z"));
  }

  @Test
  void createTaskReturnsCreatedWithLocationHeader() throws Exception {
    UUID id = UUID.randomUUID();
    when(taskService.createTask(any(CreateTaskRequest.class))).thenReturn(sampleResponse(id));
    String body =
        objectMapper.writeValueAsString(
            new CreateTaskRequest("Write report", "Quarterly", null, LocalDate.now().plusDays(3)));

    mockMvc
        .perform(post("/api/v1/tasks").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "/api/v1/tasks/" + id))
        .andExpect(jsonPath("$.id").value(id.toString()))
        .andExpect(jsonPath("$.title").value("Write report"));
  }

  @Test
  void createTaskRejectsABlankTitleWithAFieldError() throws Exception {
    String body = "{\"title\":\"   \",\"description\":\"x\"}";

    mockMvc
        .perform(post("/api/v1/tasks").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.path").value("/api/v1/tasks"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("title"))
        .andExpect(jsonPath("$.fieldErrors[0].message").value("title is required"));
  }

  @Test
  void createTaskRejectsATitleOverOneHundredCharacters() throws Exception {
    String longTitle = "a".repeat(101);
    String body = "{\"title\":\"" + longTitle + "\"}";

    mockMvc
        .perform(post("/api/v1/tasks").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("title"))
        .andExpect(
            jsonPath("$.fieldErrors[0].message").value("title must be at most 100 characters"));
  }

  @Test
  void createTaskRejectsADueDateInThePast() throws Exception {
    String body =
        "{\"title\":\"Write report\",\"dueDate\":\"" + LocalDate.now().minusDays(1) + "\"}";

    mockMvc
        .perform(post("/api/v1/tasks").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("dueDate"))
        .andExpect(jsonPath("$.fieldErrors[0].message").value("dueDate must not be in the past"));
  }

  @Test
  void createTaskReportsEveryInvalidFieldAtOnce() throws Exception {
    String body = "{\"title\":\"\",\"dueDate\":\"" + LocalDate.now().minusDays(2) + "\"}";

    mockMvc
        .perform(post("/api/v1/tasks").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors.length()").value(2))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("dueDate"))
        .andExpect(jsonPath("$.fieldErrors[1].field").value("title"));
  }

  @Test
  void createTaskRejectsAnUnknownStatusValueAsAFieldError() throws Exception {
    String body = "{\"title\":\"Write report\",\"status\":\"ALMOST_DONE\"}";

    mockMvc
        .perform(post("/api/v1/tasks").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.status").value(400))
        .andExpect(jsonPath("$.path").value("/api/v1/tasks"))
        .andExpect(jsonPath("$.fieldErrors.length()").value(1))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("status"))
        .andExpect(
            jsonPath("$.fieldErrors[0].message").value("must be one of: TODO, IN_PROGRESS, DONE"));
  }

  @Test
  void updateTaskRejectsAnUnknownStatusValueAsAFieldError() throws Exception {
    String body = "{\"title\":\"New\",\"status\":\"ALMOST_DONE\"}";

    mockMvc
        .perform(
            put("/api/v1/tasks/{id}", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("status"))
        .andExpect(
            jsonPath("$.fieldErrors[0].message").value("must be one of: TODO, IN_PROGRESS, DONE"));
  }

  @Test
  void createTaskRejectsAnUnparseableDueDateAsAFieldError() throws Exception {
    String body = "{\"title\":\"Write report\",\"dueDate\":\"not-a-date\"}";

    mockMvc
        .perform(post("/api/v1/tasks").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
        .andExpect(jsonPath("$.fieldErrors[0].field").value("dueDate"));
  }

  @Test
  void createTaskStillReportsTrulyMalformedJsonAsMalformed() throws Exception {
    String body = "{\"title\":";

    mockMvc
        .perform(post("/api/v1/tasks").contentType(MediaType.APPLICATION_JSON).content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("MALFORMED_REQUEST"))
        .andExpect(jsonPath("$.fieldErrors").doesNotExist());
  }

  @Test
  void getTaskReturnsNotFoundWithTheSharedErrorShape() throws Exception {
    UUID missingId = UUID.randomUUID();
    when(taskService.getTask(missingId)).thenThrow(new TaskNotFoundException(missingId));

    mockMvc
        .perform(get("/api/v1/tasks/{id}", missingId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("TASK_NOT_FOUND"))
        .andExpect(jsonPath("$.status").value(404))
        .andExpect(jsonPath("$.message").value("No task exists with id " + missingId))
        .andExpect(jsonPath("$.path").value("/api/v1/tasks/" + missingId))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  void getTaskRejectsAnIdThatIsNotAUuid() throws Exception {
    mockMvc
        .perform(get("/api/v1/tasks/{id}", "not-a-uuid"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
  }

  @Test
  void listTasksPassesTheStatusFilterThrough() throws Exception {
    when(taskService.listTasks(eq(TaskStatus.DONE), any(Pageable.class)))
        .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0, true));

    mockMvc
        .perform(get("/api/v1/tasks").param("status", "DONE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isArray())
        .andExpect(jsonPath("$.page").value(0));

    verify(taskService).listTasks(eq(TaskStatus.DONE), any(Pageable.class));
  }

  @Test
  void listTasksRejectsAnUnknownStatusFilter() throws Exception {
    mockMvc
        .perform(get("/api/v1/tasks").param("status", "ALMOST_DONE"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.code").value("INVALID_PARAMETER"));
  }

  @Test
  void updateTaskReturnsNotFoundForAnUnknownTask() throws Exception {
    UUID missingId = UUID.randomUUID();
    when(taskService.updateTask(eq(missingId), any()))
        .thenThrow(new TaskNotFoundException(missingId));
    String body = "{\"title\":\"New\",\"status\":\"DONE\"}";

    mockMvc
        .perform(
            put("/api/v1/tasks/{id}", missingId)
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("TASK_NOT_FOUND"));
  }

  @Test
  void updateTaskRequiresAStatus() throws Exception {
    String body = "{\"title\":\"New\"}";

    mockMvc
        .perform(
            put("/api/v1/tasks/{id}", UUID.randomUUID())
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.fieldErrors[0].field").value("status"))
        .andExpect(jsonPath("$.fieldErrors[0].message").value("status is required"));
  }

  @Test
  void deleteTaskReturnsNoContent() throws Exception {
    UUID id = UUID.randomUUID();

    mockMvc.perform(delete("/api/v1/tasks/{id}", id)).andExpect(status().isNoContent());

    verify(taskService).deleteTask(id);
  }

  @Test
  void deleteTaskReturnsNotFoundForAnUnknownTask() throws Exception {
    UUID missingId = UUID.randomUUID();
    doThrow(new TaskNotFoundException(missingId)).when(taskService).deleteTask(missingId);

    mockMvc
        .perform(delete("/api/v1/tasks/{id}", missingId))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.code").value("TASK_NOT_FOUND"));
  }
}
