# todo-ex-sample プロジェクト方針

このファイルは **todo-ex-sample 固有の方針** をまとめる。
モノレポ全体の方針（コラボレーションルール / 共通スタック / ディレクトリ規約 / `.claude/rules/*.md`）は
ルートの [`../CLAUDE.md`](../CLAUDE.md) を参照する。

---

## 位置付け

todo-sample をベースに発展課題を盛り込む。
**REST API・Flyway・オニオン + DDD・Yavi による always-valid・関数型** を学習目的の中心に据える。

---

## プレゼンテーション

- **REST API**（リクエスト/レスポンスはすべて JSON）
- Thymeleaf は使わない

## マイグレーション

- **Flyway**
- マイグレーションは Spring アプリと **独立した Gradle ビルド** `todo-ex-sample-migration/` で管理する
  （詳細は `todo-ex-sample-migration/` 参照）

---

## アーキテクチャ: オニオン + DDD

- **4 層構成**: Domain / Application / Infrastructure / Presentation
- 依存方向は **常に Domain に向かう**。Domain は他層に依存しない

### パッケージ構成: モジュール（Bounded Context）ファースト

```
com.example.todo_ex_sample.<module>.<layer>.<subpackage>
```

4 層はモジュール内に並ぶ。例:

- `todo.domain.model.Todo`
- `todo.domain.repository.TodoRepository`
- `todo.application.usecase.CreateTodoUseCase`
- `todo.infrastructure.persistence.mybatis.TodoMyBatisRepository`
- `todo.presentation.rest.TodoController`

現状はモジュール 1 つだが、最初からこの構成で揃える。

### ドメインオブジェクトの命名

- モジュール内の VO / エンティティは **コンテキスト接頭辞を付けない**
  - 例: `todo.domain.model.Title` / `Detail` / `Todo`
  - 集約ルートはモジュール名と一致してよい（`Todo`）
- ただし **識別子 ID は `TodoId` のようにコンテキストを冠する**（曖昧さを避けるため）

---

## VO / Always-valid（Yavi）

Java `record` + **Yavi** で「生成時に必ず妥当」(always valid) を担保する。

- 生成口は **`public static Validated<T> of(...)` ファクトリ** に統一する
  - 内部で `VALIDATOR.applicative().validate(new T(...))` を呼び、`Validated<T>` を返す
- `new T(...)` の **直接呼び出しはコード規約で禁止**（レビューで担保）
  - `of` を介すことで「妥当な値しか外に出さない」状態を作る
- **コンパクトコンストラクタでの `throwIfInvalid()` 方式は採用しない**
  - 例外型になるとエラー集約（`Validations.apply` での複数 VO 同時検証）ができなくなるため
- Validator の `constraint(getter, name, ...)` の **name には「外から見たフィールドの意味」** を入れる
  - `"value"` の共通使い回し禁止
  - 集約のエラー出力で違反箇所を区別できるようにするため
  - 例: `Title` は `"title"`、`Detail` は `"detail"`
- **集約ルートの生成も `Todo.of(...)` ファクトリで `Validated<Todo>` を返し**、
  `Validations.apply(...)` で配下 VO の違反をまとめて集約する

---

## 関数型プログラミング志向

- 不変（`final` / `record`）
- 副作用の分離
- `Optional`、`Validated<T>`、`Stream` などの活用
- 例外スローは境界層（Presentation 層の例外ハンドラなど）に寄せる

---

## テスト方針

- **UT / IT を明確に分離**
  - UT: ドメイン層 / アプリケーション層の純粋ロジック
  - IT: インフラ層（Repository 実装）/ Presentation 層は **Testcontainers で実 PostgreSQL** を起動
- テストはレイヤごとの責務に沿って書く
  - Domain → 純粋ロジックの UT
  - Infrastructure → IT 中心

### テストメソッド命名

- メソッド名は **英語 lowerCamelCase**。Java 識別子に日本語は使わない
- 読みやすい説明は **`@DisplayName("正常系: 境界の255文字")`** で付ける
- カテゴリ区切りはアンダースコア許容: `valid_oneChar` / `invalid_null` など

---

## 固有依存（Spring Initializr + 手動追記）

Initializr では最小構成で生成し、生成後に手動で `build.gradle` に追記する:

- `am.ik.yavi:yavi`（VO / Validated）
- Flyway 系は **マイグレーション専用サブビルド** `todo-ex-sample-migration/` 側に持つ。
  Spring アプリ本体には Flyway 依存を入れない
- Testcontainers（IT 着手時に追加）
- MyBatis Spring Boot Starter（インフラ層着手時に追加）
- Spring Web（REST API 着手時に追加）

`build.gradle` には **固有の依存のみ** を書く。Java バージョン・Spring Boot バージョン・
共通テスト依存などはルートで一元管理しているので再宣言しない。

---

## DB 接続

- ルートの `compose.yaml` で起動する PostgreSQL コンテナの `todo_ex_sample` データベースに接続する
- スキーマ管理は `todo-ex-sample-migration/` の Flyway で完結させる
- 接続情報は `src/main/resources/application.yml` に書く
