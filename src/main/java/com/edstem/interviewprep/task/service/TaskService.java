package com.edstem.interviewprep.task.service;

import com.edstem.interviewprep.common.dto.response.PageResponse;
import com.edstem.interviewprep.task.dto.request.CreateTaskRequest;
import com.edstem.interviewprep.task.dto.request.UpdateTaskRequest;
import com.edstem.interviewprep.task.dto.response.TaskResponse;
import com.edstem.interviewprep.task.entity.Task;
import com.edstem.interviewprep.task.entity.TaskStatus;
import com.edstem.interviewprep.task.exception.TaskNotFoundException;
import com.edstem.interviewprep.task.mapper.TaskMapper;
import com.edstem.interviewprep.task.repository.TaskRepository;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskService {

  private final TaskRepository taskRepository;

  public TaskService(TaskRepository taskRepository) {
    this.taskRepository = taskRepository;
  }

  @Transactional
  public TaskResponse createTask(CreateTaskRequest request) {
    Task task =
        Task.create(
            request.title(), request.description(), request.statusOrDefault(), request.dueDate());
    return TaskMapper.toResponse(taskRepository.save(task));
  }

  @Transactional(readOnly = true)
  public PageResponse<TaskResponse> listTasks(TaskStatus status, Pageable pageable) {
    Page<Task> tasks =
        status == null
            ? taskRepository.findAll(pageable)
            : taskRepository.findByStatus(status, pageable);
    return PageResponse.from(tasks.map(TaskMapper::toResponse));
  }

  @Transactional(readOnly = true)
  public TaskResponse getTask(UUID id) {
    return TaskMapper.toResponse(findTaskOrThrow(id));
  }

  @Transactional
  public TaskResponse updateTask(UUID id, UpdateTaskRequest request) {
    Task task = findTaskOrThrow(id);
    task.update(request.title(), request.description(), request.status(), request.dueDate());
    return TaskMapper.toResponse(task);
  }

  @Transactional
  public void deleteTask(UUID id) {
    taskRepository.delete(findTaskOrThrow(id));
  }

  private Task findTaskOrThrow(UUID id) {
    return taskRepository.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
  }
}
