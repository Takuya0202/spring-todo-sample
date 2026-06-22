package com.example.todo_sample.repository;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.example.todo_sample.entity.Todo;

@Mapper
public interface TodoMapper {
    List<Todo> selectAll();

    // メソッド引数が1つなら@paramは省略してもいい。
    Todo selectById(@Param("id") Integer id);

    void insert(Todo todo);

    void update(Todo todo);

    void delete(@Param("id") Integer id);
}
