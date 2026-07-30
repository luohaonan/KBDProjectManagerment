package com.kbd.pms.web;

import com.kbd.pms.dto.UnifiedPendingTodoDto;
import com.kbd.pms.service.TodoAggregationService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/todos")
public class TodoController {

  private final TodoAggregationService todoAggregationService;

  public TodoController(TodoAggregationService todoAggregationService) {
    this.todoAggregationService = todoAggregationService;
  }

  @GetMapping("/pending")
  public Result<List<UnifiedPendingTodoDto>> getPendingTodos() {
    return Result.ok(todoAggregationService.getPendingTodosForCurrentUser());
  }
}