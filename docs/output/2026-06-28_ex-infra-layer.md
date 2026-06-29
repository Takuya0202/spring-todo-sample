# 2026-06-28 ex-sample Infrastructure 層 実装 進行ログ

対応 plan: [`../input/2026-06-28_ex-infra-layer.md`](../input/2026-06-28_ex-infra-layer.md)

## ステップ進捗

| # | ステップ | 状態 |
|---|---|---|
| 0 | docs/CLAUDE.md 整備 (input/output 運用開始) | ✅ 完了 |
| 1 | `todo-ex-sample/build.gradle` 追記 | ✅ 完了 |
| 2 | `application.properties` 追記 | ✅ 完了 |
| 3 | `Todo` 集約に `change` メソッド追加 | 🟡 進行中 |
| 4 | `DomainIntegrityException` 作成 | ⏳ 着手前 |
| 5 | `TodoRepository` interface 作成 | ⏳ 着手前 |
| 6 | `TodoRow` (record + `toDomain()`) 作成 | ⏳ 着手前 |
| 7 | `TodoMapper` (interface) 作成 | ⏳ 着手前 |
| 8 | `TodoMapper.xml` 作成 | ⏳ 着手前 |
| 9 | `TodoMyBatisRepository` 作成 | ⏳ 着手前 |
| 10 | `src/test/resources/schema.sql` 作成 | ⏳ 着手前 |
| 11 | `TodoRowTest` (UT) 作成 | ⏳ 着手前 |
| 12 | `TodoMyBatisRepositoryIT` 作成 | ⏳ 着手前 |
| - | 検証 (`./gradlew :todo-ex-sample:test`) | ⏳ 着手前 |

---

## Step 0: docs / CLAUDE.md 整備 — 完了

### やったこと

- `docs/input/` `docs/output/` の 2 ディレクトリを新設
- 既存の `docs/2026-06-22_flyway-independent-gradle.md` / `docs/2026-06-22_gradle-postgres-setup.md` / `docs/2026-06-27_ex-domain-vo-and-roadmap.md` を `git mv` で `docs/output/` 配下に移動
- `docs/input/2026-06-28_ex-infra-layer.md` を作成 (本セッションの plan を正本として残す)
- 本ファイル (`docs/output/2026-06-28_ex-infra-layer.md`) を新規作成 (進行ログとして以降追記)
- `CLAUDE.md` の「作業状況」セクションを書き換え。input / output の役割と新規セッション着手時の読む順序を明文化

### 学んだこと / 合意

- **input vs output** の役割分離: input は plan の「不変なスナップショット」、output は「累積する進行ログ」
- input が大きく変わる場合は **末尾に補遺セクション** or **新規 input ファイル** で対応 (上書きしない)
- 古い `docs/2026-06-*` も output 性質として `docs/output/` 配下に集約済み

### 次のステップ

→ **Step 1**: `todo-ex-sample/build.gradle` に MyBatis Starter / PG Driver / Testcontainers 系の依存を追加する案を AI が提示し、ユーザが手で書き写す。

---

## Step 1: `todo-ex-sample/build.gradle` 追記 — 完了

### やったこと

- `todo-ex-sample/build.gradle` の `dependencies { ... }` に Infrastructure 層用の依存を追記:
  - `implementation 'org.mybatis.spring.boot:mybatis-spring-boot-starter:3.0.5'`
  - `runtimeOnly 'org.postgresql:postgresql'`
  - `testImplementation 'org.testcontainers:junit-jupiter'`
  - `testImplementation 'org.testcontainers:postgresql'`
  - `testImplementation 'org.mybatis.spring.boot:mybatis-spring-boot-starter-test:3.0.5'`
- ルートに集約された Spring Boot BOM 経由で PostgreSQL ドライバと Testcontainers のバージョンが解決される

### 学んだこと / 合意

- **`runtimeOnly`** は実行時のみ必要な依存に使う絞り口。PostgreSQL JDBC ドライバはコンパイル時に型を直接参照しないので `implementation` より絞れる
- Spring Boot BOM (`io.spring.dependency-management`) は **直接の依存だけでなく `testcontainers` などの周辺ライブラリのバージョン**も解決する。明示的なバージョンを書かなくて済む
- 本体には Flyway を入れない (マイグレーションはサブビルド側の責務)。Spring 起動時に DB スキーマがあることは前提

### 次のステップ

→ **Step 2**: `src/main/resources/application.properties` に PostgreSQL 接続情報と MyBatis Mapper XML パスを追記する案を提示する。

---

## Step 2: `application.properties` 追記 — 完了

### やったこと

- `spring.datasource.url=jdbc:postgresql://localhost:5432/todo_ex_sample` ほか接続情報を追記
- `mybatis.mapper-locations=classpath:mybatis.mapper/*.xml` (todo-sample 流儀に合わせる)
- `logging.level.com.example.todo_ex_sample.todo.infrastructure=DEBUG` で Mapper SQL ログを有効化

### 学んだこと / 合意

- **`spring.sql.init.mode=always` は入れない**。ex-sample のスキーマ管理は Flyway サブビルドが担当するので、Spring の起動時 SQL 初期化機能と被らせない (両方が走ると順序事故が起きる)
- Mapper XML パス規約は `todo-sample` と揃える: classpath ルート直下の `mybatis.mapper/` ディレクトリ。サブごとに違う規約を抱えると認知コストが上がる

### 次のステップ

→ **Step 3**: `Todo` 集約に状態遷移メソッド `change(String, String) → Validated<Todo>` を追加する案を提示する。Domain 既存ファイルへの初の編集。
