package com.example.todo_ex_sample.todo.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import am.ik.yavi.core.Validated;

class DetailTest {

    @Test
    @DisplayName("正常系: 最小の1文字")
    void valid_oneChar() {
        Validated<Detail> result = Detail.of("a");

        assertThat(result.isValid()).isTrue();
        assertThat(result.value().value()).isEqualTo("a");
    }

    @Test
    @DisplayName("正常系: 境界の2000文字")
    void valid_boundary2000() {
        String input = "a".repeat(2000);

        Validated<Detail> result = Detail.of(input);

        assertThat(result.isValid()).isTrue();
    }

    @Test
    @DisplayName("異常系: null")
    void invalid_null() {
        Validated<Detail> result = Detail.of(null);

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors().get(0).name()).isEqualTo("detail");
    }

    @Test
    @DisplayName("異常系: 空白のみ")
    void invalid_blank() {
        Validated<Detail> result = Detail.of("   ");

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors().get(0).name()).isEqualTo("detail");
    }

    @Test
    @DisplayName("異常系: 2001文字 / 長さ超過")
    void invalid_tooLong2001() {
        Validated<Detail> result = Detail.of("a".repeat(2001));

        assertThat(result.isValid()).isFalse();
        assertThat(result.errors().get(0).name()).isEqualTo("detail");
    }
}
