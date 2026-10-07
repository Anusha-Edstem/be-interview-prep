package com.edstem.interviewprep.task.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.edstem.interviewprep.common.dto.response.PageResponse;
import com.edstem.interviewprep.task.dto.request.CreateTaskRequest;
import com.edstem.interviewprep.task.dto.request.UpdateTaskRequest;
import com.edstem.interviewprep.task.dto.response.TaskResponse;
import com.edstem.interviewprep.task.entity.Task;
import com.edstem.interviewprep.task.entity.TaskStatus;
import com.edstem.interviewprep.task.exception.TaskNotFoundException;
import com.edstem.interviewprep.task.repository.TaskRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

  @Mock private TaskRepository taskRepository;

  @InjectMocks private TaskService taskService;

  @Test
  void createTaskDefaultsStatusToTodoWhenAbsent() {
    CreateTaskRequest request =
        new CreateTaskRequest("Write report", "Quarterly", null, LocalDate.now().plusDays(3));
    when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

    TaskResponse response = taskService.createTask(request);

    ArgumentCaptor<Task> saved = ArgumentCaptor.forClass(Task.class);
    verify(taskRepository).save(saved.capture());
    assertThat(saved.getValue().getStatus()).isEqualTo(TaskStatus.TODO);
    assertThat(response.title()).isEqualTo("Write report");
  }

  @Test
  void createTaskKeepsTheRequestedStatus() {
    CreateTaskRequest request =
        new CreateTaskRequest("Ship it", null, TaskStatus.IN_PROGRESS, null);
    when(taskRepository.save(any(Task.class))).thenAnswer(invocation -> invocation.getArgument(0));

    TaskResponse response = taskService.createTask(request);

    assertThat(response.status()).isEqualTo(TaskStatus.IN_PROGRESS);
  }

  @Test
  void getTaskThrowsWhenTheTaskDoesNotExist() {
    UUID missingId = UUID.randomUUID();
    when(taskRepository.findById(missingId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> taskService.getTask(missingId))
        .isInstanceOf(TaskNotFoundException.class)
        .hasMessageContaining(missingId.toString());
  }

  @Test
  void listTasksFiltersByStatusWhenOneIsGiven() {
    Pageable pageable = PageRequest.of(0, 20);
    Task task = Task.create("Review PR", null, TaskStatus.DONE, null);
    when(taskRepository.findByStatus(TaskStatus.DONE, pageable))
        .thenReturn(new PageImpl<>(List.of(task), pageable, 1));

    PageResponse<TaskResponse> response = taskService.listTasks(TaskStatus.DONE, pageable);

    assertThat(response.content()).hasSize(1);
    assertThat(response.totalElements()).isEqualTo(1);
    verify(taskRepository, never()).findAll(pageable);
  }

  @Test
  void listTasksReturnsEveryTaskWhenNoStatusIsGiven() {
    Pageable pageable = PageRequest.of(0, 20);
    when(taskRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(), pageable, 0));

    PageResponse<TaskResponse> response = taskService.listTasks(null, pageable);

    assertThat(response.content()).isEmpty();
    verify(taskRepository, never()).findByStatus(any(), any());
  }

  @Test
  void updateTaskOverwritesEveryMutableField() {
    UUID id = UUID.randomUUID();
    Task existing = Task.create("Old title", "Old", TaskStatus.TODO, null);
    when(taskRepository.findById(id)).thenReturn(Optional.of(existing));
    LocalDate dueDate = LocalDate.now().plusDays(1);

    TaskResponse response =
        taskService.updateTask(
            id, new UpdateTaskRequest("New title", "New", TaskStatus.DONE, dueDate));

    assertThat(response.title()).isEqualTo("New title");
    assertThat(response.description()).isEqualTo("New");
    assertThat(response.status()).isEqualTo(TaskStatus.DONE);
    assertThat(response.dueDate()).isEqualTo(dueDate);
  }

  @Test
  void deleteTaskThrowsWhenTheTaskDoesNotExist() {
    UUID missingId = UUID.randomUUID();
    when(taskRepository.findById(missingId)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> taskService.deleteTask(missingId))
        .isInstanceOf(TaskNotFoundException.class);
    verify(taskRepository, never()).delete(any(Task.class));
  }
}
