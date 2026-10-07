package com.edstem.interviewprep.task.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.edstem.interviewprep.task.entity.Task;
import com.edstem.interviewprep.task.entity.TaskStatus;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TaskRepositoryTest {

  @Autowired private TaskRepository taskRepository;

  @Test
  void findByStatusReturnsOnlyTasksInThatStatus() {
    taskRepository.save(Task.create("Draft spec", null, TaskStatus.TODO, null));
    taskRepository.save(Task.create("Build it", null, TaskStatus.IN_PROGRESS, null));
    taskRepository.save(Task.create("Ship it", null, TaskStatus.DONE, null));

    Page<Task> inProgress =
        taskRepository.findByStatus(TaskStatus.IN_PROGRESS, PageRequest.of(0, 20));

    assertThat(inProgress.getTotalElements()).isEqualTo(1);
    assertThat(inProgress.getContent().get(0).getTitle()).isEqualTo("Build it");
  }

  @Test
  void flushingANewTaskPopulatesTheGeneratedIdAndCreatedDate() {
    Task saved =
        taskRepository.saveAndFlush(
            Task.create("Draft spec", "A spec", TaskStatus.TODO, LocalDate.now().plusDays(5)));

    assertThat(saved.getId()).isNotNull();
    assertThat(saved.getCreatedDate()).isNotNull();
  }

  @Test
  void theMigrationMatchesTheEntityMappingForEveryColumn() {
    Task saved =
        taskRepository.saveAndFlush(
            Task.create("Draft spec", "A spec", TaskStatus.DONE, LocalDate.now().plusDays(5)));

    Task reloaded = taskRepository.findById(saved.getId()).orElseThrow();

    assertThat(reloaded.getTitle()).isEqualTo("Draft spec");
    assertThat(reloaded.getDescription()).isEqualTo("A spec");
    assertThat(reloaded.getStatus()).isEqualTo(TaskStatus.DONE);
    assertThat(reloaded.getDueDate()).isEqualTo(LocalDate.now().plusDays(5));
    assertThat(reloaded.getCreatedDate()).isNotNull();
  }
}
