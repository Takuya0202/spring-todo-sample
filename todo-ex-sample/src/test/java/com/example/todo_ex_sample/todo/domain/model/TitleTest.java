package com.example.todo_ex_sample.todo.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import am.ik.yavi.core.Validated;

public class TitleTest {
    @Test
    @DisplayName("正常系：最小1文字")
    void valid_oneChar() {
        Validated<Title> result = Title.of("a");
        assertThat(result.isValid()).isTrue();
        assertThat(result.value().value()).isEqualTo("a");
    }

    @Test
    @DisplayName("正常系: 境界の255文字")
    void valid_boundary255() {
        String input = "a".repeat(255);

        Validated<Title> result = Title.of(input);

        assertThat(result.isValid()).isTrue();
        assertThat(result.value().value()).isEqualTo(input);
    }

    @Test
    @DisplayName("異常系: null")
    void invalid_null() {
        Validated<Title> result = Title.of(null);

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors()).hasSize(1);
        assertThat(result.errors().get(0).name()).isEqualTo("title");
    }

    @Test
    @DisplayName("異常系: 空文字")
    void invalid_empty() {
        Validated<Title> result = Title.of("");

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors().get(0).name()).isEqualTo("title");
    }

    @Test
    @DisplayName("異常系: 空白のみ")
    void invalid_blank() {
        Validated<Title> result = Title.of("   ");

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors().get(0).name()).isEqualTo("title");
    }

    @Test
    @DisplayName("異常系: 256文字 / 長さ超過")
    void invalid_tooLong256() {
        Validated<Title> result = Title.of("a".repeat(256));

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors().get(0).name()).isEqualTo("title");
    }
}
