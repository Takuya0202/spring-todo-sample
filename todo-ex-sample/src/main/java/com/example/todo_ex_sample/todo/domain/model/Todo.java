package com.example.todo_ex_sample.todo.domain.model;

import java.time.LocalDateTime;
import java.util.Optional;

import am.ik.yavi.core.Validated;
import am.ik.yavi.fn.Validations;

public record Todo(
        TodoId todoId,
        Title title,
        Optional<Detail> detail,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
    // 新しくtodoを作る。VOを集約したクラス。
    public static Validated<Todo> create(String rawTitle, String rawDetail) {
        Validated<Title> titleV = Title.of(rawTitle);

        // detailはnull / blank を許可。存在する場合はDetail型であること
        Validated<Optional<Detail>> detailV = (rawDetail == null || rawDetail.isBlank())
                ? Validated.successWith(Optional.empty())
                : Detail.of(rawDetail).map(Optional::of);

        // 集約クラスなので、applyで全て判断して返す。
        return Validations.apply(
                (title, detail) -> {
                    LocalDateTime now = LocalDateTime.now();
                    return new Todo(TodoId.generate(), title, detail, now, now);
                },
                titleV, detailV);
    }

    public static Todo of(
            TodoId todoId,
            Title title,
            Optional<Detail> detail,
            LocalDateTime createdAt,
            LocalDateTime updatedAt) {
        return new Todo(todoId, title, detail, createdAt, updatedAt);
    }
}
