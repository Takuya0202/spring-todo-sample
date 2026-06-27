package com.example.todo_sample.service.impl;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.todo_sample.entity.Todo;
import com.example.todo_sample.repository.TodoMapper;
import com.example.todo_sample.service.TodoService;

@Service
// service層にtransactionを張る。
// runtimeExceptionが発生すると自動的にrollbackされる。
@Transactional
public class TodoServiceImpl implements TodoService {
    private final TodoMapper todoMapper;

    TodoServiceImpl(TodoMapper todoMapper) {
        this.todoMapper = todoMapper;
    }

    @Override
    public List<Todo> findAllTodo() {
        return todoMapper.selectAll();
    }

    @Override
    public Todo findByIdTodo(Integer id) {
        return todoMapper.selectById(id);
    }

    @Override
    public void insertTodo(Todo todo) {
        todoMapper.insert(todo);
    }

    @Override
    public void updateTodo(Todo todo) {
        todoMapper.update(todo);
    }

    @Override
    public void deleteTodo(Integer id) {
        todoMapper.delete(id);
    }
}
