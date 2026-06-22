package com.example.todo_sample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.example.todo_sample.entity.Todo;
import com.example.todo_sample.repository.TodoMapper;

@SpringBootApplication
public class TodoSampleApplication {

	public static void main(String[] args) {
		SpringApplication.run(TodoSampleApplication.class, args)
				.getBean(TodoSampleApplication.class).exec();
	}

	// demo確認
	private final TodoMapper todoMapper;

	// di
	public TodoSampleApplication(TodoMapper todoMapper) {
		this.todoMapper = todoMapper;
	}

	public void exec() {
		System.out.println("run selectAll");

		for (Todo row : todoMapper.selectAll()) {
			System.out.println(row);
		}

		System.out.println("run selectById");
		System.out.println(todoMapper.selectById(1));

		System.out.println("create new task");
		Todo todo = new Todo();
		todo.setTodo("test");
		todo.setDetail("detail");
		todoMapper.insert(todo);
		System.out.println("check new task exist");
		System.out.println(todoMapper.selectById(3));

		Todo target = todoMapper.selectById(1);
		target.setTodo("updated test");
		target.setDetail("updated detail");
		todoMapper.update(target);
		System.out.println("check updated task success");
		System.out.println(todoMapper.selectById(3));

		todoMapper.delete(3);
		System.out.println("check delete task success");
		for (Todo row : todoMapper.selectAll()) {
			System.out.println(row);
		}
	}
}
