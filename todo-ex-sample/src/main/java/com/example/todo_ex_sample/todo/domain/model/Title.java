package com.example.todo_ex_sample.todo.domain.model;

import am.ik.yavi.builder.ValidatorBuilder;
import am.ik.yavi.core.Validated;
import am.ik.yavi.core.Validator;

public record Title(String value) {
  private static final Validator<Title> VALIDATOR = ValidatorBuilder.<Title>of()
      .constraint(Title::value, "title", c -> c.notBlank().lessThanOrEqual(255))
      .build();

  // record生成時をinstance生成せずにof()のファクトリメソッドで生成することを守る。
  // そうすることでalways-validを守ることができる。生成側はorElseThrow()で例外をキャッチする
  public static Validated<Title> of(String value) {
    return VALIDATOR.applicative().validate(new Title(value));
  }
}
