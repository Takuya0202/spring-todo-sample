# 2026-06-28 ex-sample Infrastructure 層 実装 plan (Phase 1)

## Context

`todo-ex-sample` は前セッションで Domain VO 群 (`Title` / `Detail` / `TodoId` / `Todo` 集約) と Flyway マイグレーション (V1.0 → V1.3 で `id UUID PK / title varchar(255) / detail TEXT nullable / created_at NOT NULL / updated_at NOT NULL` の最終形に到達) が完成済み。
今セッションで参考書側 `todo-sample` のドメイン層・インフラ層が完了したのを受け、ex-sample の **Infrastructure 層** に着手する。

CLAUDE.md / memory 既定の方針を維持:

- **Phase 1 (素朴 + 関数型の入口)** を崩さない。`Todo.create(String, String) → Validated<Todo>` / `Todo.of(VO...) → Todo` の 2 ファクトリは残す
- **AI はコードを書かない**。**チャット上のコードブロックで提示**するのみ。ユーザが手で書いて学習する
- 副作用 (`UUID.randomUUID()` / `LocalDateTime.now()`) は **Domain 内で取得** (Clock 注入は YAGNI)
- Repository は **`TodoRow` + `toDomain()` パターン**
- IT は **Testcontainers** で実 PostgreSQL を起動 (Application 起動側は別ビルドの Flyway 適用済み前提)

## このセッションで合意した設計判断

| 論点 | 決定 |
|---|---|
| スコープ | **Infrastructure 層まで** (Application 層は Phase 2 進化トリガーとして別セッション) |
| `DomainIntegrityException` 配置 | **`todo.domain.exception`** (Domain 不変条件違反として概念上 Domain 所属。`RuntimeException` 直継承で依存追加なし) |
| `TodoRow.toDomain()` で `detail` が blank | **`Optional.empty()` 扱い** (`Todo.create` の挙動と対称。fail させない) |
| `Todo` 集約への update 系メソッド | **`change(String rawTitle, String rawDetail) → Validated<Todo>` 1 本**。`updatedAt = LocalDateTime.now()` を Domain 内で取り、新しい不変 `Todo` を返す。集約に generic な `update` は載せない (Ubiquitous Language) |
| IT のスキーマ初期化 | **Testcontainers の `withInitScript("schema.sql")`** で `src/test/resources/schema.sql` を適用。Spring 設定は汚さない。`schema.sql` 冒頭に「V1.3 適用後の最終形・手動同期」コメントを付ける |
| MyBatis のパラメータ型 | `Todo` を直接渡さない (`Optional<Detail>` を MyBatis に解釈させない)。`TodoMapper` インターフェースで **`@Param` フラット引数** に分解 (`UUID id, String title, String detail, LocalDateTime createdAt, LocalDateTime updatedAt`)。Repository 実装内で `Optional<Detail>` → `String or null` に変換 |
| UUID マッピング | PostgreSQL JDBC ドライバが `java.util.UUID ↔ uuid` をネイティブ対応するため特別対応なし。挙動不審なら `#{id, jdbcType=OTHER}` 付与で対処 |
| IT のテストアノテーション | `@MybatisTest` ではなく `@SpringBootTest` (`@Repository` Bean を Spring が読む必要あり) |

### 議論で深掘りした概念 (memo)

- **貧血ドメインモデル** = 「データだけ持って振る舞いを持たない」状態。集約に `update(...)` 名のメソッドが「あること自体」が貧血の指標ではない。中身にビジネスロジックが入っていれば貧血ではない
- **Ubiquitous Language** = メソッド名は業務担当者の語彙に揃える。`update` / `change` は CRUD 語彙寄り、`rename` / `editDetail` / `complete` は業務語彙寄り。今回の Todo は title/detail に「業務的に意味のある区別」が無いので Phase 1 では `change(title, detail)` 1 本で十分
- **Repository の `update(Todo)` は永続化操作** であって業務遷移ではない。集約 (`Todo.change`) で遷移 → Repository が DB に書き戻す、という分担
- **Application 層の UseCase** は「load → transition → persist」のオーケストレーション。遷移ロジック自体は集約に置く

## 提示シーケンス (12 ステップ)

> 各ステップは **AI がチャット上で提示** → **ユーザが手で書く** という流れ。
> 提示時は (ファイルパス / 目的 / コード or 構造スケッチ / テスト観点) のセットで出す。
> 各ステップ完了ごとに AI が `docs/output/2026-06-28_ex-infra-layer.md` に進行ログを追記する。

### 1. `todo-ex-sample/build.gradle` 追記

- **目的**: MyBatis Starter / PG Driver / Testcontainers を追加。本体に Flyway は入れない (マイグレーションは別サブビルドが担当)
- **追加内容**:
  - `implementation 'org.mybatis.spring.boot:mybatis-spring-boot-starter:3.0.5'`
  - `runtimeOnly 'org.postgresql:postgresql'`
  - `testImplementation 'org.testcontainers:junit-jupiter'`
  - `testImplementation 'org.testcontainers:postgresql'`
  - `testImplementation 'org.mybatis.spring.boot:mybatis-spring-boot-starter-test:3.0.5'`
  - (Testcontainers バージョンは Spring Boot BOM の `testcontainers.version` 解決に任せる)
- **確認**: `./gradlew :todo-ex-sample:dependencies` で BOM 解決が通ること

### 2. `todo-ex-sample/src/main/resources/application.properties` 追記

- **目的**: PostgreSQL 接続と MyBatis Mapper XML パス
- **内容**: `spring.datasource.url=jdbc:postgresql://localhost:5432/todo_ex_sample` / `username=todo` / `password=todo` / `spring.datasource.driver-class-name=org.postgresql.Driver` / `mybatis.mapper-locations=classpath:mybatis.mapper/*.xml` / `logging.level.com.example.todo_ex_sample.todo.infrastructure=DEBUG`
- **確認**: 後続ステップで Spring 起動が成立すること

### 3. `Todo` 集約に `change` メソッド追加

- **配置**: 既存 `todo.domain.model.Todo` を編集
- **目的**: 状態遷移メソッド (Phase 1)。create と対称形で title + detail を同時遷移し `updatedAt` を Domain 内で更新
- **シグネチャ**: `public Validated<Todo> change(String rawTitle, String rawDetail)`
- **本体スケッチ**: `Title.of(rawTitle)` と `(rawDetail == null || rawDetail.isBlank()) ? Validated.successWith(Optional.empty()) : Detail.of(rawDetail).map(Optional::of)` を `Validations.apply((title, detail) -> new Todo(this.todoId, title, detail, this.createdAt, LocalDateTime.now()), titleV, detailV)` で集約 (`todoId` と `createdAt` は不変)
- **UT 観点**: title だけ変更 / detail だけ変更 / 両方変更 / title 不正で `failure` / `updatedAt` が前より進む / `todoId` と `createdAt` が保たれる

### 4. `todo.domain.exception.DomainIntegrityException`

- **配置**: 新規 `todo/domain/exception/DomainIntegrityException.java`
- **目的**: 「DB に妥当でない値が居る = ドメイン不変条件違反」を fail-fast で示す例外
- **構造**: `public final class DomainIntegrityException extends RuntimeException`。コンストラクタ `(String message)` と `(String message, Throwable cause)` の 2 種。Yavi 依存禁止 (Domain 純粋性)
- **UT 観点**: 不要 (純データクラス)。利用は次以降のステップで確認

### 5. `todo.domain.repository.TodoRepository`

- **配置**: 新規 `todo/domain/repository/TodoRepository.java`
- **目的**: Domain 境界の窓口。`Validated<T>` を外に出さない契約を型で示す
- **シグネチャ**:
  ```java
  Optional<Todo> findById(TodoId id);
  List<Todo> findAll();
  void save(Todo todo);
  void update(Todo todo);          // 永続化レイヤの書き戻し。集約の状態遷移は呼び出し側で完了している前提
  void deleteById(TodoId id);
  ```
- **UT 観点**: 直接の UT なし。IT で実装を駆動する

### 6. `todo.infrastructure.persistence.mybatis.TodoRow`

- **配置**: 新規 `todo/infrastructure/persistence/mybatis/TodoRow.java`
- **目的**: DB 行そのままを表す record。always-valid 担保の翻訳点 (`toDomain()`)
- **構造**:
  ```java
  public record TodoRow(UUID id, String title, String detail,
                        LocalDateTime createdAt, LocalDateTime updatedAt) {
      public Todo toDomain() { ... }
  }
  ```
- **`toDomain()` 実装方針**:
  - `TodoId todoId = TodoId.of(id).orElseThrow(v -> new DomainIntegrityException("invalid todoId: " + v));`
  - `Title titleVo = Title.of(title).orElseThrow(v -> new DomainIntegrityException("invalid title: " + v));`
  - **detail blank → empty**: `Optional<Detail> detailVo = (detail == null || detail.isBlank()) ? Optional.empty() : Optional.of(Detail.of(detail).orElseThrow(v -> new DomainIntegrityException("invalid detail: " + v)));`
  - `Todo.of(todoId, titleVo, detailVo, createdAt, updatedAt)` を返す
- **UT 観点** (推奨): null detail / blank detail → `Optional.empty()` / 不正 title (空文字) → `DomainIntegrityException` / 正常ケース

### 7. `todo.infrastructure.persistence.mybatis.TodoMapper` (interface)

- **配置**: 新規 `todo/infrastructure/persistence/mybatis/TodoMapper.java`
- **目的**: MyBatis Mapper 境界。`TodoRow` で打ち止め、Domain 型 (`Todo` / VO) は触れない
- **シグネチャ**:
  ```java
  @Mapper
  public interface TodoMapper {
      Optional<TodoRow> selectById(@Param("id") UUID id);
      List<TodoRow> selectAll();
      void insert(@Param("id") UUID id, @Param("title") String title, @Param("detail") String detail,
                  @Param("createdAt") LocalDateTime createdAt, @Param("updatedAt") LocalDateTime updatedAt);
      void update(@Param("id") UUID id, @Param("title") String title, @Param("detail") String detail,
                  @Param("updatedAt") LocalDateTime updatedAt);
      void deleteById(@Param("id") UUID id);
  }
  ```
- **判断点**: `update` は `createdAt` を SET しない (不変条件)。SQL 側でも `created_at` は触らない

### 8. `todo-ex-sample/src/main/resources/mybatis.mapper/TodoMapper.xml`

- **目的**: SQL 定義。`as createdAt` / `as updatedAt` で snake_case → record コンポーネント名へエイリアス (todo-sample 流儀、resultMap なし)
- **構造スケッチ**:
  - `namespace="com.example.todo_ex_sample.todo.infrastructure.persistence.mybatis.TodoMapper"`
  - `<select id="selectById" resultType="com.example.todo_ex_sample.todo.infrastructure.persistence.mybatis.TodoRow">select id, title, detail, created_at as createdAt, updated_at as updatedAt from todos where id = #{id}</select>`
  - 同形で `selectAll`
  - `<insert id="insert">insert into todos (id, title, detail, created_at, updated_at) values (#{id}, #{title}, #{detail}, #{createdAt}, #{updatedAt})</insert>`
  - `<update id="update">update todos set title = #{title}, detail = #{detail}, updated_at = #{updatedAt} where id = #{id}</update>`
  - `<delete id="deleteById">delete from todos where id = #{id}</delete>`
- **方針**: `current_timestamp` は SQL 側で取らない (時刻は Domain が握る)

### 9. `todo.infrastructure.persistence.mybatis.TodoMyBatisRepository`

- **配置**: 新規 `todo/infrastructure/persistence/mybatis/TodoMyBatisRepository.java`
- **目的**: `TodoRepository` 実装。`Optional<Detail>` → nullable String 変換と `TodoRow → Todo` 橋渡しのみ
- **構造**:
  ```java
  @Repository
  public class TodoMyBatisRepository implements TodoRepository {
      private final TodoMapper mapper;
      // コンストラクタ DI
      public Optional<Todo> findById(TodoId id) {
          return mapper.selectById(id.value()).map(TodoRow::toDomain);
      }
      public List<Todo> findAll() {
          return mapper.selectAll().stream().map(TodoRow::toDomain).toList();
      }
      public void save(Todo todo) {
          mapper.insert(todo.todoId().value(), todo.title().value(),
                        todo.detail().map(Detail::value).orElse(null),
                        todo.createdAt(), todo.updatedAt());
      }
      public void update(Todo todo) {
          mapper.update(todo.todoId().value(), todo.title().value(),
                        todo.detail().map(Detail::value).orElse(null),
                        todo.updatedAt());
      }
      public void deleteById(TodoId id) {
          mapper.deleteById(id.value());
      }
  }
  ```
- **判断点**: `save` / `update` は集約をそのまま受け取り、内部で VO を分解。呼び出し側 (将来の UseCase) は集約だけ知っていれば良い

### 10. `todo-ex-sample/src/test/resources/schema.sql`

- **目的**: Testcontainers がコンテナ起動時に流すスキーマ。V1.0 → V1.3 の最終形を手動同期
- **内容**:
  ```sql
  -- V1.0-V1.3 適用後の最終 DDL。
  -- todo-ex-sample-migration/db/migration/ と手動同期すること。
  -- schema.sql のドリフトを検知するテストは将来追加検討。
  create table todos (
      id          uuid primary key,
      title       varchar(255) not null,
      detail      text,
      created_at  timestamp not null,
      updated_at  timestamp not null
  );
  ```

### 11. `TodoRowTest` (UT) — 任意だが推奨

- **配置**: 新規 `src/test/java/com/example/todo_ex_sample/todo/infrastructure/persistence/mybatis/TodoRowTest.java`
- **目的**: `toDomain()` の VO 化ロジックを純関数 UT で固める (IT より速いフィードバック)
- **ケース** (`@DisplayName` 日本語 / メソッド英語):
  - `toDomain_validRow_buildsTodo()` — 正常系
  - `toDomain_nullDetail_returnsEmpty()` — `Optional.empty()` になる
  - `toDomain_blankDetail_returnsEmpty()` — blank も empty 扱い (合意済み)
  - `toDomain_blankTitle_throwsDomainIntegrityException()`
  - `toDomain_nullId_throwsDomainIntegrityException()`

### 12. `TodoMyBatisRepositoryIT` (IT)

- **配置**: 新規 `src/test/java/com/example/todo_ex_sample/todo/infrastructure/persistence/mybatis/TodoMyBatisRepositoryIT.java`
- **構造**:
  - `@SpringBootTest` + `@Testcontainers`
  - `@Container static PostgreSQLContainer<?> pg = new PostgreSQLContainer<>("postgres:16-alpine").withInitScript("schema.sql");`
  - `@DynamicPropertySource` で `spring.datasource.url/username/password` を上書き
  - `@Autowired TodoRepository repository;` (interface 越し) と `@Autowired TodoMapper mapper;` (corrupt row 注入用)
- **ケース** (`@DisplayName` 日本語 / メソッド英語):
  - `save_thenFindById_returnsSameTodo()` — `Todo.create("買い物","牛乳").value()` 保存 → 同値復元
  - `save_withoutDetail_persistsAsNull()` — `Todo.create("買い物", null).value()` → 復元時 `detail().isEmpty()`
  - `findById_unknown_returnsEmpty()` — ランダム UUID → `Optional.empty()`
  - `update_changesTitleAndUpdatedAt()` — `Todo.change("買い物リスト", "卵")` を呼んだ集約を `update` → 復元値が一致、`updatedAt` が前進
  - `deleteById_removesRow()`
  - **`findById_corruptRow_throwsDomainIntegrityException()`** — `mapper.insert(id, "", null, now, now)` (title 空文字) を直接 INSERT → `repository.findById(id)` が `DomainIntegrityException`。**always-valid 境界の核**

## 検証 (12 ステップ完了後)

1. `docker compose up -d` で PostgreSQL を起動
2. (一回) `todo-ex-sample-migration` で本番 DB に V1.0–V1.3 を流しておく (前セッション完了済みなら不要)
3. `./gradlew :todo-ex-sample:test` — UT (Domain 既存 + `change` 追加分 + `TodoRow`) と IT 全パス
4. `./gradlew :todo-ex-sample:bootRun` — Spring 起動成立 (アプリ自体は REST を未実装なので bean 起動の確認まで)
5. IT が `corruptRow_throwsDomainIntegrityException` で fail-fast していることをログ確認

## 進めかた

ユーザが各提示を順に手で書き写し、コンパイル & テストを通したら次のステップへ。
途中で疑問が出たら都度議論 → 合意後に提示内容を補正する。
完了後、Application 層 (Phase 2 進化トリガー) の着手判断は次セッションに委ねる。

## 参考ファイル

- 既存 Domain: `todo-ex-sample/src/main/java/com/example/todo_ex_sample/todo/domain/model/Todo.java` (step 3 で編集)
- 参考書側の MyBatis 流儀: `todo-sample/src/main/resources/mybatis.mapper/TodoMapper.xml` / `todo-sample/src/main/java/com/example/todo_sample/repository/TodoMapper.java`
- スキーマ同期元: `todo-ex-sample-migration/db/migration/V1.3__refine_columns.sql`
