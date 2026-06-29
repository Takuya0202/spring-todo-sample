# 2026-06-22 セッション: Flyway を独立 Gradle ビルドで導入

## このセッションのスコープ

- todo-ex-sample のマイグレーション基盤を Flyway で構築
- Spring アプリと完全に切り離した独立 Gradle ビルドとして実装

## 完了したこと

### 独立 Gradle ビルド `todo-ex-sample-migration/` を新設

- モノレポルートとは `settings.gradle` で完全分離（include しない）
- Wrapper はモノレポルートから `gradlew` / `gradlew.bat` / `gradle/wrapper/` をコピー
- 自身の `settings.gradle` を 1 行で配置することで、`./gradlew` が親 settings に上昇するのを防止
- ディレクトリ構成:

  ```text
  todo-ex-sample-migration/
  ├── settings.gradle          # rootProject.name = '...' の 1 行
  ├── build.gradle             # Flyway プラグイン + DB classpath
  ├── gradlew, gradlew.bat
  ├── gradle/wrapper/
  └── db/migration/
      ├── V1.0__init_schema.sql
      └── V1.1__seed_data.sql
  ```

### Flyway プラグイン設定（buildscript 方式）

- 当初 `configurations { flywayMigration } / flyway { configurations = [...] }` 方式で書いたが、`java` プラグイン未適用の素プロジェクトでは `No database found to handle jdbc:postgresql://...` で失敗
- `buildscript { dependencies { classpath ... } }` でプラグイン本体の classpath に直接乗せる方式に切り替えて動作
- 採用バージョン: Flyway 10.20.1 / Postgres JDBC 42.7.4

### todo-ex-sample の Spring 依存をクリーンに保つ

- 一時的に追加した Flyway 系依存（`flyway-core` / `flyway-database-postgresql` / `spring-boot-starter-jdbc`）を撤回
- Spring アプリ側は `spring-boot-starter` のみに戻し、Flyway は完全にマイグレーション専用サブの責務へ

## 議論して理解した内容（個人ノートは obsidian で管理）

- Flyway は Spring 非依存。Spring Boot 統合は便利機能の 1 つにすぎず、本番運用では CI/CD で `flywayMigrate` → アプリデプロイ の分離が定番
- Flyway のバージョン番号は `V1.0` / `V1.1` / `V20260622103000` など自由形式。数値比較
- Gradle Wrapper の正体（gradlew + gradlew.bat + gradle-wrapper.jar + .properties の 4 ファイル）と、Spring Initializr が単に同梱しているだけという関係
- Gradle インストール / バージョン管理（SDKMAN / mise）の選択肢と「Wrapper があれば基本不要」という現代の常識
- 独立 Gradle ビルドにおける `settings.gradle` の役割（親への探索停止マーカー）
- `No database found to handle` エラーの正体は「クラスパスに DB モジュールが無い」であって接続エラーではない

→ 詳細は obsidian の `catch-up/flyway.md` に集約

## 未解決・確認待ち

- なし（マイグレーション基盤としては完結）

## 次のセッションでやること

**メインテーマ**: todo-sample でドメイン層 / インフラ層を実装し、その後 todo-ex-sample でオニオン + DDD + Yavi の文脈に翻訳して応用する。

順序:

1. **todo-sample 側**:
   - ドメイン層: `Todo` クラス（参考書の構成・命名に従う、Lombok 活用）
   - インフラ層: MyBatis Mapper による Todo の CRUD
   - 3 層構成（アプリ / ドメイン / インフラ）の責務分離を体感
2. **todo-ex-sample 側**:
   - ドメイン層: `Todo` を `record` で再設計、Yavi で生成時バリデーション（always valid）
   - 値オブジェクト（VO）の切り出し（`TodoId` / `TodoTitle` / `TodoDetail` など）
   - インフラ層: ドメインオブジェクト ↔ DB レコードの変換責務
   - オニオンアーキテクチャの依存方向（Domain ← Application ← Infrastructure）を守る

CLAUDE.md の「todo-sample 先行 → todo-ex-sample へ翻訳」順序を維持する。
