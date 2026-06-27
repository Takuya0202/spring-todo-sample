package com.example.todo_ex_sample.todo.domain.model;

import java.util.UUID;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.core.Validated;
import am.ik.yavi.core.Validator;

public record TodoId(UUID value) {

    // Stringはconstraintを使うがそれ以外は_integerなど用途に合わせる、存在しない場合は_object
    private static final Validator<TodoId> VALIDATOR = ValidatorBuilder.<TodoId>of()
            ._object(TodoId::value, "todoId", c -> c.notNull())
            .build();

    public static TodoId generate() {
        // UUID.randomUUID() は仕様上必ず妥当な UUID を返すので Validated を介さない
        return new TodoId(UUID.randomUUID());
    }

    public static Validated<TodoId> of(UUID value) {
        return VALIDATOR.applicative().validate(new TodoId(value));
    }
}
