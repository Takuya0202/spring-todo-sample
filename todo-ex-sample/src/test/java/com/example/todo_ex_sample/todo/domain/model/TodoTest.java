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

    // change はインスタンスメソッドなので「遷移前 Todo」を用意してから呼ぶ。
    // updatedAt は now() が動くので、遷移前を意図的に過去にして「前進したこと」を主張する。
    private Todo aTodoCreatedDayAgo() {
        TodoId id = TodoId.generate();
        Title title = Title.of("買い物").value();
        Optional<Detail> detail = Optional.of(Detail.of("牛乳").value());
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 27, 10, 0);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 6, 27, 10, 0);
        return Todo.of(id, title, detail, createdAt, updatedAt);
    }

    @Test
    @DisplayName("change: 正常系 - title と detail 両方更新")
    void change_validBoth_returnsUpdatedTodo() {
        Todo before = aTodoCreatedDayAgo();

        Validated<Todo> result = before.change("買い物リスト", "卵");

        assertThat(result.isValid()).isTrue();
        Todo after = result.value();
        assertThat(after.title().value()).isEqualTo("買い物リスト");
        assertThat(after.detail()).isPresent();
        assertThat(after.detail().get().value()).isEqualTo("卵");
    }

    @Test
    @DisplayName("change: 正常系 - detail を空 (null) に消す")
    void change_emptyDetail_becomesEmpty() {
        Todo before = aTodoCreatedDayAgo();

        Validated<Todo> result = before.change("買い物", null);

        assertThat(result.isValid()).isTrue();
        assertThat(result.value().detail()).isEmpty();
    }

    @Test
    @DisplayName("change: 正常系 - detail を空白文字列で消しても empty")
    void change_blankDetail_becomesEmpty() {
        Todo before = aTodoCreatedDayAgo();

        Validated<Todo> result = before.change("買い物", "   ");

        assertThat(result.isValid()).isTrue();
        assertThat(result.value().detail()).isEmpty();
    }

    @Test
    @DisplayName("change: 異常系 - title が空白で failure")
    void change_invalidTitle_returnsFailure() {
        Todo before = aTodoCreatedDayAgo();

        Validated<Todo> result = before.change("", "卵");

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).name()).isEqualTo("title");
    }

    @Test
    @DisplayName("change: 異常系 - title と detail 両方違反は集約される")
    void change_invalidBoth_errorsAggregated() {
        Todo before = aTodoCreatedDayAgo();

        Validated<Todo> result = before.change("", "a".repeat(2001));

        assertThat(result.isValid()).isFalse();
        var names = result.errors().stream().map(v -> v.name()).toList();
        assertThat(names).containsExactlyInAnyOrder("title", "detail");
    }

    @Test
    @DisplayName("change: todoId と createdAt は遷移後も保たれる")
    void change_preservesTodoIdAndCreatedAt() {
        Todo before = aTodoCreatedDayAgo();

        Todo after = before.change("買い物リスト", "卵").value();

        assertThat(after.todoId()).isEqualTo(before.todoId());
        assertThat(after.createdAt()).isEqualTo(before.createdAt());
    }

    @Test
    @DisplayName("change: updatedAt は遷移前より前進する")
    void change_advancesUpdatedAt() {
        Todo before = aTodoCreatedDayAgo(); // updatedAt は 2026-06-27 10:00 固定

        Todo after = before.change("買い物リスト", "卵").value();

        // before.updatedAt は過去固定値、after.updatedAt は LocalDateTime.now() なので
        // 厳格に「より後」を主張できる。
        assertThat(after.updatedAt()).isAfter(before.updatedAt());
    }

}
