# 2026-06-27 セッション: ex のドメイン層着手と DDD ロードマップ整理

## このセッションのスコープ

- todo-ex-sample のドメイン層（VO・集約）を着手
- Yavi + `Validated<T>` で always-valid を担保する設計の検討
- CLAUDE.md のサブ階層分離（モノレポ全体方針 vs サブ固有方針）
- DDD の流派とフェーズ別ロードマップの策定

---

## 完了したこと

### マイグレーション SQL の修正

- `V1.2__swap_pk_to_uuid.sql` の `exsits` → `exists` typo 修正
- `V1.3__refine_columns.sql` の `ALTER TABLE` 構文修正（`;` で文が切れていた）

### CLAUDE.md のサブ階層分離

ルート CLAUDE.md が肥大化していたため、固有方針をサブに分離:

- ルート: モノレポ全体方針 / 共通スタック / ディレクトリ規約 / `.claude/rules/*.md` 参照
- `todo-sample/CLAUDE.md`: 3 層 / MVC + Thymeleaf / 参考書写経方針 / 固有依存
- `todo-ex-sample/CLAUDE.md`: REST / Flyway / オニオン + DDD / モジュールファースト / Yavi `Validated<T>` / 関数型 / テスト命名

Claude Code は階層下の CLAUDE.md を自動でマージ読み込みするので、サブ配下で作業すれば固有方針が自然に反映される。

### Yavi 導入と最初の VO 群

`todo-ex-sample/build.gradle` に `am.ik.yavi:yavi:0.14.1` を追加。

ドメイン層に 3 つの VO + 集約ルートを実装（モジュールファースト構成 `todo.domain.model`）:

- `Title` (`record Title(String value)`) — `notBlank` + `lessThanOrEqual(255)`
- `Detail` (`record Detail(String value)`) — `notBlank` + `lessThanOrEqual(2000)`
- `TodoId` (`record TodoId(UUID value)`) — `_object(...).notNull()`、`generate()` と `of(UUID)` の 2 ファクトリ
- `Todo` (集約ルート) — `create(String, String)` で `Validations.apply` 合成 / `of(VO...)` で組み立て

各 VO は **`public static Validated<T> of(...)` ファクトリ** に統一。`new` 直叩きは規約で禁止。`Validator` の `name` には外から見たフィールドの意味（`"title"` / `"detail"` / `"todoId"`）を入れる。

UT は AssertJ + `@DisplayName` で日本語表示。メソッド名は英語 lowerCamelCase。

---

## 議論して理解した内容（詳細は obsidian で個別管理）

### Yavi の制約 API

- `constraint(getter, name, c -> ...)` は **String 専用**。他の型はそれぞれ `_integer` / `_long` / `_object` 等
- 第 2 引数の `name` は **エラーメッセージで「どこが落ちたか」を示す識別子**。`Validations.apply` での集約合成で複数 VO の違反を区別するために、フィールドの意味（`"title"` / `"detail"`）を入れる。`"value"` の共通使い回しは NG
- `.applicative()` を挟むことで他の `Validated` と合成可能になる

### Yavi 単独だと届かない領域の扱い

- 軽量な追加条件: `.predicate(述語, messageKey, message)` で済ます
- 例外を投げる Java 標準処理（`UUID.fromString` / `LocalDate.parse`）: try/catch で `Validated.failure(...)` に詰め替える
- VO に「内部用 (`of(VO 型)`)」と「外部入力用 (`ofString(String)`)」の 2 つの口を持たせる発想

### Repository 境界での always-valid 担保

- DB は完全に信頼できない（マイグレーション差分・直 SQL・他システム・テストデータ）ため、Infrastructure 層が VO 化を必ず通す必要がある
- 採用パターン: **`TodoRow` + `toDomain()`**
  - `TodoRow` = DB 行の構造そのまま（MyBatis の resultType）
  - `toDomain()` 内で各 VO の `of(...).orElseThrow(...)` を呼び、`Todo.of(...)` で組み立てる
  - Repository は薄く `Optional.ofNullable(mapper.findById(...)).map(TodoRow::toDomain)` だけ
- 不正値は専用例外 `DomainIntegrityException` で fail-fast。「値が無い」(`Optional.empty()`) と「値が不正」(例外) は扱いを分ける

### DDD の流派

- Evans（古典）/ Vernon（実践）/ Wlaschin（関数型）で実装が異なる
- 我々の路線は Vernon + Wlaschin のハイブリッド
- 「`reconstruct` を分けるか」「Domain が String を受けるか VO を受けるか」「副作用を Domain 内に閉じるか Application 層に出すか」はすべて流派ごとに違う
- 学習目的のため、**フェーズ 1（素朴）→ フェーズ 2（関数型 DDD の本領）** で段階的に進化させる方針に決定

### コラボレーションルールの更新

- 設計判断は **議論を尽くしてから方針ドキュメントに記載**（合意未済の独断記載を反省）
- ドキュメント編集は事前確認不要（既に明示的に合意した方針のみ）
- テスト識別子は英語 lowerCamelCase、説明は `@DisplayName`

→ 詳細は memory（user/feedback 記憶）に保存

---

## ロードマップ（フェーズ 1 → 2）

### フェーズ 1（現在）: 素朴 + 関数型の入口

- `Todo.create(String, String)` で副作用と `Validations.apply` を Domain 内に閉じる
- `Todo.of(VO...)` を Repository（`TodoRow.toDomain()`）と Domain 内部から呼ぶ
- `reconstruct` という名前は使わない（`of` 一本）
- 副作用（UUID / now）は Domain 内で取得（Clock 注入は YAGNI）
- Repository は `TodoRow` + `toDomain()` パターン

### フェーズ 2（必要になったら）

進化トリガー:
- テストで時刻 / UUID を固定したくなった
- Application 層（UseCase）を作るタイミング
- 複数 UseCase で共通の入力解釈ロジックが現れた

進化内容:
- `Todo.create(String, String)` を削除し `Todo.of(VO...)` 一本に統一
- `Validations.apply` と String → VO の正規化を Application 層に移す
- `Clock` / `IdGenerator` を引数注入してテスタブルに

---

## 未解決・確認待ち

- なし（フェーズ 1 として整合性は取れている）

---

## 次のセッションでやること

ユーザ希望: **todo-sample のドメイン層・インフラ層を先に完成させる** → その後 ex-sample に戻る。

### 次セッション前半: todo-sample 側

CLAUDE.md の「todo-sample 先行 → ex-sample へ翻訳」順序に立ち戻る。todo-sample のドメイン層 / インフラ層が中途半端なので完成させる:

- ドメイン層: 参考書通りの `Todo` クラス（Lombok 活用、エンティティ素朴）
- インフラ層: MyBatis Mapper による Todo の CRUD
- アプリケーション層 / プレゼンテーション層（MVC + Thymeleaf）まで通すかは参考書の進行に合わせる

### 次セッション後半: ex-sample に戻る

todo-sample の完成を踏まえて、ex-sample の続き:

1. **`TodoRow` + `toDomain()` パターン**を Infrastructure 層に実装
   - `todo.infrastructure.persistence.mybatis.TodoRow`
   - `todo.infrastructure.persistence.mybatis.TodoMapper`（MyBatis Mapper）
   - `todo.infrastructure.persistence.mybatis.TodoMyBatisRepository`
2. **`DomainIntegrityException`** の置き場所決め（候補: `todo.domain.exception` or `todo.infrastructure.exception`）
3. **`TodoRepository` インタフェース**を Domain 層に置く（`todo.domain.repository`）
4. **MyBatis Spring Boot Starter 依存**を `build.gradle` に追加
5. **Testcontainers での IT** を準備（IT は Infrastructure 層から）

その先（フェーズ 2 への着手判断）:
- Application 層（UseCase）を作るタイミングで、`Validations.apply` と String → VO 正規化を Domain から Application に移す
- REST API（Controller）作成
