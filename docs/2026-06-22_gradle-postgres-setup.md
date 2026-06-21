# 2026-06-22 セッション: Gradle 基盤と PostgreSQL セットアップ

## このセッションのスコープ

- Gradle マルチプロジェクト基盤の整備
- PostgreSQL の compose 構成と接続確認
- todo-sample の DB 接続設定の修正

## 完了したこと

### Gradle 基盤
- ルートの `settings.gradle` / `build.gradle` でマルチプロジェクト化
- Spring Boot プラグインと `io.spring.dependency-management` をルートで一元管理
- 各サブの `build.gradle` を「依存追加だけ」のスリム構成に
- Wrapper はルートに集約後、各サブにもコピー（どちらからでも起動可能に）

### Spring Initializr 生成物
- todo-sample / todo-ex-sample を Initializr で生成し、不要ファイルを整理
- 既存方針は CLAUDE.md に明文化済み

### PostgreSQL
- compose.yaml: 1 コンテナ・2 DB 構成（`todo_sample` / `todo_ex_sample` + 管理用 `postgres`）
- `db/init/create-database.sql` で初回起動時に業務 DB を生成
- todo-sample の `application.properties` を `todo_sample` に接続するよう修正

## 議論して理解した内容（個人ノートは obsidian で管理）

- PostgreSQL の階層: クラスタ（≒インスタンス）/ DB / スキーマ / テーブル
- 「クラスタ」の用語が文脈で意味が変わる（PostgreSQL 固有 vs 一般用語）
- マイクロサービスの DB 分離はインスタンス / DB / スキーマの 3 段階
- ロールはインスタンス単位だが、DB ごとに別ユーザー・別パスワードを実現可能
- CQRS と Read Replica の違い

## 未解決・確認待ち

- [ ] todo-sample の `./gradlew :todo-sample:bootRun` が DB 接続修正後に起動するか
- [ ] todo-sample の `schema.sql` / `data.sql` がコミット未済
- [ ] todo-sample の `Makefile` がコミット未済

## 次のセッションでやること

**メインテーマ**: todo-sample 側で `schema.sql` / `data.sql` を使ったマイグレーション風の流れを、todo-ex-sample では **Flyway** で実装する。

具体的には:

1. todo-ex-sample に Flyway 依存を追加（`flyway-core` / `flyway-database-postgresql`）
2. `application.properties` で `todo_ex_sample` への接続を設定
3. `src/main/resources/db/migration/` 配下に Flyway のマイグレーションファイルを配置
   - `V1__init_schema.sql`（todo-sample の `schema.sql` 相当）
   - `V2__seed_data.sql`（todo-sample の `data.sql` 相当）
4. 起動確認 → Flyway が自動で適用されること、`flyway_schema_history` テーブルが作られることを確認

CLAUDE.md の todo-ex-sample 固有方針には既に Flyway 採用が記載済みなので、方針追加は不要。
