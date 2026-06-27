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
- Yavi の組み込み制約で表現しきれないバリデーション（例外を投げる Java 標準処理 / 複合条件 等）は
  **自前で `Validated.successWith(...)` / `Validated.failure(...)` を組み立てて返してよい**。
  - 軽量な追加条件は Yavi の `.predicate(述語, messageKey, message)` で済ませる
  - `UUID.fromString` / `LocalDate.parse` のように **例外を投げる前処理**は
    Java 標準で行い、try/catch で `Validated.failure(...)` に詰め替える

### 集約ルートのファクトリ

集約ルート（`Todo`）の生成口は **2 種類**:

- **`create(rawTitle, rawDetail)`**: 新規作成。外部の生値から検証し、
  `Validations.apply(...)` で配下 VO の違反をまとめて集約。`id` / `createdAt` / `updatedAt` を内部で生成。
  戻り値は `Validated<Todo>`
- **`of(id, title, detail, createdAt, updatedAt)`**: 既に妥当な VO から組み立てる共通口。
  Repository（`TodoRow.toDomain()`）と Domain 内部から呼ぶ。戻り値は `Todo` 直
- `create` と `of` の 2 口でフェーズ 1 は十分。`reconstruct` という名前は使わない
- 副作用（`UUID.randomUUID()` / `LocalDateTime.now()`）は **フェーズ 1 では Domain 内で取得**。
  将来 Application 層を作って `Validations.apply` をそちらへ移すタイミングで、
  `Clock` 注入や `Todo.of(VO...)` への一本化を検討する（フェーズ 2）

### Repository 境界での always-valid 担保

DB は完全に信頼できない（マイグレーション差分・直 SQL・他システム書込み・テストデータの手抜き等）ため、
**Infrastructure 層が「DB の生値 → VO」の変換で必ず `of(...)` を経由する** ことで always-valid を守る。

- **`TodoRow` + `toDomain()` パターン**を採用:
  - `TodoRow` は **DB 行の構造そのままを表す record**（MyBatis の resultType として使う）。
    フィールドは UUID / String / LocalDateTime など Java 標準型
  - `toDomain()` メソッドで **VO 化と集約組み立てを一箇所に集約**:
    各 VO の `of(...).orElseThrow(...)` を呼び、`Todo.of(...)` で組み立てて返す
  - Repository 実装は薄く、`TodoRow` を取り出して `toDomain()` を呼ぶだけ
- Repository の戻り値型は **`Optional<Todo>`** など業務処理側が扱いやすい純粋な集約。
  `Validated<T>` を業務処理層に流さない（入れ子地獄を避ける）
- 不正値は `Title.of(raw).orElseThrow(violations -> new DomainIntegrityException(...))` のように
  **専用のドメイン例外で fail-fast**。「値が無い」(`Optional.empty()`) と「値が不正」(例外) は扱いを分ける
- `TodoRow.toDomain()` 内の `Todo.of(...)` 呼び出しは VO ファクトリ経由なので **`new` 直叩きにあたらない**

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
