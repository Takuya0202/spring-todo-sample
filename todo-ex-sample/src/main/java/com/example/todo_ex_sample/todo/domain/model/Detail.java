package com.example.todo_ex_sample.todo.domain.model;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.core.Validated;
import am.ik.yavi.core.Validator;

public record Detail(String value) {
    private static final int MAX_LENGTH = 2000;

    private final static Validator<Detail> VALIDATOR = ValidatorBuilder.<Detail>of()
            .constraint(Detail::value, "detail", c -> c.notBlank().lessThanOrEqual(MAX_LENGTH))
            .build();

    public static Validated<Detail> of(String value) {
        return VALIDATOR.applicative().validate(new Detail(value));
    }
}
