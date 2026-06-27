package com.example.todo_sample.service;

import java.util.List;

import com.example.todo_sample.entity.Todo;

public interface TodoService {
    List<Todo> findAllTodo();

    Todo findByIdTodo(Integer id);

    void insertTodo(Todo todo);

    void updateTodo(Todo todo);

    void deleteTodo(Integer id);
}
