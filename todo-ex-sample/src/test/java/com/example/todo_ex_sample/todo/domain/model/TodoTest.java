package com.example.todo_ex_sample.todo.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import am.ik.yavi.core.Validated;

class TodoTest {

    @Test
    @DisplayName("create: 正常系 - title と detail あり")
    void create_validWithDetail() {
        Validated<Todo> result = Todo.create("買い物", "牛乳とパン");

        assertThat(result.isValid()).isTrue();
        Todo todo = result.value();
        assertThat(todo.todoId()).isNotNull();
        assertThat(todo.title().value()).isEqualTo("買い物");
        assertThat(todo.detail()).isPresent();
        assertThat(todo.detail().get().value()).isEqualTo("牛乳とパン");
        assertThat(todo.createdAt()).isEqualTo(todo.updatedAt());
    }

    @Test
    @DisplayName("create: 正常系 - detail なし (null)")
    void create_validWithoutDetail_null() {
        Validated<Todo> result = Todo.create("買い物", null);

        assertThat(result.isValid()).isTrue();
        assertThat(result.value().detail()).isEmpty();
    }

    @Test
    @DisplayName("create: 正常系 - detail なし (空白のみ)")
    void create_validWithoutDetail_blank() {
        Validated<Todo> result = Todo.create("買い物", "   ");

        assertThat(result.isValid()).isTrue();
        assertThat(result.value().detail()).isEmpty();
    }

    @Test
    @DisplayName("create: 異常系 - title が空白のみ")
    void create_invalidTitle() {
        Validated<Todo> result = Todo.create("", "詳細");

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).name()).isEqualTo("title");
    }

    @Test
    @DisplayName("create: 異常系 - detail が長さ超過")
    void create_invalidDetail() {
        Validated<Todo> result = Todo.create("買い物", "a".repeat(2001));

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).name()).isEqualTo("detail");
    }

    @Test
    @DisplayName("create: 異常系 - title と detail 両方違反 → 違反が集約される")
    void create_invalidBoth_errorsAggregated() {
        Validated<Todo> result = Todo.create("", "a".repeat(2001));

        assertThat(result.isValid()).isFalse();
        var names = result.errors().stream().map(v -> v.name()).toList();
        assertThat(names).containsExactlyInAnyOrder("title", "detail");
    }

    @Test
    @DisplayName("of: 既に妥当な VO を渡して Todo を組み立てる")
    void of_buildsTodoFromValidVOs() {
        TodoId id = TodoId.generate();
        Title title = Title.of("タイトル").value();
        Optional<Detail> detail = Optional.of(Detail.of("詳細").value());
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 1, 0, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 1, 2, 0, 0);

        Todo todo = Todo.of(id, title, detail, createdAt, updatedAt);

        assertThat(todo.todoId()).isEqualTo(id);
        assertThat(todo.title()).isEqualTo(title);
        assertThat(todo.detail()).isEqualTo(detail);
        assertThat(todo.createdAt()).isEqualTo(createdAt);
        assertThat(todo.updatedAt()).isEqualTo(updatedAt);
    }
}
