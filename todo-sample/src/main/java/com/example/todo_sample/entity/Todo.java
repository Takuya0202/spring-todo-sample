package com.example.todo_sample.entity;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
// mybatisの取得でデフォルトコンストラクタが必要になる。仕様
@NoArgsConstructor
@AllArgsConstructor
public class Todo {
    private Integer id;

    private String todo;

    private String detail;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
