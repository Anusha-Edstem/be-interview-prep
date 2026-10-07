package com.edstem.interviewprep.task.controller;

import com.edstem.interviewprep.common.dto.response.PageResponse;
import com.edstem.interviewprep.task.dto.request.CreateTaskRequest;
import com.edstem.interviewprep.task.dto.request.UpdateTaskRequest;
import com.edstem.interviewprep.task.dto.response.TaskResponse;
import com.edstem.interviewprep.task.entity.TaskStatus;
import com.edstem.interviewprep.task.service.TaskService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {

  private final TaskService taskService;

  public TaskController(TaskService taskService) {
    this.taskService = taskService;
  }

  @PostMapping
  public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody CreateTaskRequest request) {
    TaskResponse created = taskService.createTask(request);
    return ResponseEntity.created(URI.create("/api/v1/tasks/" + created.id())).body(created);
  }

  @GetMapping
  public ResponseEntity<PageResponse<TaskResponse>> listTasks(
      @RequestParam(required = false) TaskStatus status,
      @PageableDefault(size = 20, sort = "createdDate", direction = Sort.Direction.DESC)
          Pageable pageable) {
    return ResponseEntity.ok(taskService.listTasks(status, pageable));
  }

  @GetMapping("/{id}")
  public ResponseEntity<TaskResponse> getTask(@PathVariable UUID id) {
    return ResponseEntity.ok(taskService.getTask(id));
  }

  @PutMapping("/{id}")
  public ResponseEntity<TaskResponse> updateTask(
      @PathVariable UUID id, @Valid @RequestBody UpdateTaskRequest request) {
    return ResponseEntity.ok(taskService.updateTask(id, request));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> deleteTask(@PathVariable UUID id) {
    taskService.deleteTask(id);
    return ResponseEntity.noContent().build();
  }
}
