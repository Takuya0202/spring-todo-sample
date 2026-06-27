package com.example.todo_sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.example.todo_sample.entity.Todo;
import com.example.todo_sample.service.TodoService;

@SpringBootApplication
public class TodoSampleApplication {

	public static void main(String[] args) {
		SpringApplication.run(TodoSampleApplication.class, args)
				.getBean(TodoSampleApplication.class).exec();
	}

	// demo確認
	private final TodoService todoService;

	// di
	public TodoSampleApplication(TodoService todoService) {
		this.todoService = todoService;
	}

	public void exec() {
		System.out.println("run selectAll");

		for (Todo row : todoService.findAllTodo()) {
			System.out.println(row);
		}

		System.out.println("run selectById");
		System.out.println(todoService.findByIdTodo(1));

		System.out.println("create new task");
		Todo todo = new Todo();
		todo.setTodo("test");
		todo.setDetail("detail");
		todoService.insertTodo(todo);
		System.out.println("check new task exist");
		System.out.println(todoService.findByIdTodo(3));

		Todo target = todoService.findByIdTodo(1);
		target.setTodo("updated test");
		target.setDetail("updated detail");
		todoService.updateTodo(target);
		System.out.println("check updated task success");
		System.out.println(todoService.findByIdTodo(3));

		todoService.deleteTodo(3);
		System.out.println("check delete task success");
		for (Todo row : todoService.findAllTodo()) {
			System.out.println(row);
		}
	}
}
