package com.example.todo_ex_sample.todo.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import am.ik.yavi.core.Validated;

class TodoIdTest {

    @Test
    @DisplayName("generate: 常に valid な TodoId を生成する")
    void generate_isAlwaysValid() {
        TodoId id = TodoId.generate();

        assertThat(id.value()).isNotNull();
    }

    @Test
    @DisplayName("generate: 呼び出すたびに異なる UUID を返す")
    void generate_isUnique() {
        TodoId a = TodoId.generate();
        TodoId b = TodoId.generate();

        assertThat(a.value()).isNotEqualTo(b.value());
    }

    @Test
    @DisplayName("正常系: 既存 UUID から復元")
    void valid_fromExistingUuid() {
        UUID uuid = UUID.randomUUID();

        Validated<TodoId> result = TodoId.of(uuid);

        assertThat(result.isValid()).isTrue();
        assertThat(result.value().value()).isEqualTo(uuid);
    }

    @Test
    @DisplayName("異常系: null")
    void invalid_null() {
        Validated<TodoId> result = TodoId.of(null);

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors().get(0).name()).isEqualTo("todoId");
    }
}
