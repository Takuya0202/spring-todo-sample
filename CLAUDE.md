# CLAUDE.md

このリポジトリで Claude Code が作業するときに従う方針をまとめる。
既存の `.claude/rules/*.md` のルールはそのまま尊重し、ここでは重複させず参照する。

---

## プロジェクト概要

学習用のモノレポ。Todo アプリを 2 つ並行で育てる。

- **todo-sample**: 参考書のハンズオン写経。Spring Boot 3.5.15 / Spring MVC / Thymeleaf / MyBatis / PostgreSQL を用い、アプリケーション層・ドメイン層・インフラ層の 3 層構成で実装する。命名・構成は参考書に従う。
- **todo-ex-sample**: todo-sample をベースに発展課題を盛り込む。REST API（リクエスト/レスポンスはすべて JSON）、Flyway、オニオンアーキテクチャ + DDD、Yavi による always-valid、Java `record` による VO 厳格化、関数型プログラミング、UT/IT の充実を目的とする。共通スタックは todo-sample と同じ。

---

## 最重要のコラボレーションルール

> **AI エージェントはコードを書かない。書くべきコードを「提示」するだけ。**

- `Edit` / `Write` / `NotebookEdit` などでアプリ本体のソースを直接編集してはならない。
- 提示はチャット上のコードブロックで行う（ファイルパス・目的・差分や全文・テスト観点をセットで示す）。
- 例外: ユーザーが「これを書いて」「適用して」と明示的に指示した場合のみ、その範囲に限り編集可。
- 理由: ユーザー本人が手で書くことで理解を定着させたいため。先回りして書くと学習効果を奪う。
- メモ / 設定ファイル / `docker-compose.yml` のような土台ファイルについても、まずは提示にとどめ、ユーザーの指示を待つ。

---

## 開発ワークフロー

1. **todo-sample 先行**: あるレイヤ（最初はドメイン層）を todo-sample で完成させる。
2. **todo-ex-sample へ同期**: 同じレイヤを todo-ex-sample で再設計し、オニオン/DDD/Yavi/record の文脈に翻訳して提示する。
3. todo-ex-sample を単独で先行させない。常に todo-sample → todo-ex-sample の順。
4. AI は各ステップで「AI が提示する責務」と「ユーザーが書く責務」を明確に分けて出力する。

提示時の推奨フォーマット:

- **ファイルパス**: 配置すべき場所
- **目的**: なぜこのコードが必要か
- **コード**: 最小実装（過剰な抽象や将来用フックは入れない）
- **テスト観点**: 何を確認すべきか（UT/IT のどちらか含む）

1 ステップに複数レイヤや複数責務を詰め込まない。

---

## 共通スタック

両アプリで固定の前提（ルートで共通管理する）:

- Java 21（JDK21）
- Spring Boot 3.5.15
- PostgreSQL（Docker で起動）
- MyBatis
- Gradle（Groovy DSL、`build.gradle`）
- テスト: JUnit5、IT は Testcontainers で実 PostgreSQL を起動

これらのバージョン・プラグイン適用はルートの `build.gradle` / `settings.gradle` で一元管理する。各サブプロジェクトでは Spring Boot や Java バージョンを再宣言せず、固有の依存だけを `dependencies { ... }` に追加する。

### サブプロジェクトの初期生成

両サブは **Spring Initializr** で個別に生成する。Initializr で指定する値:

- Project: Gradle - Groovy
- Language: Java
- Spring Boot: 3.5.15
- Group: `com.example`
- Artifact: 各プロジェクト名（`todo-sample` / `todo-ex-sample`）
- Package name: Group + Artifact（`com.example.todosample` / `com.example.todoexsample`）
- Packaging: Jar
- Java: 21

#### todo-sample の依存（Initializr で選択）

- Spring Boot DevTools
- Lombok
- Spring Web
- MyBatis Framework
- Thymeleaf
- PostgreSQL Driver

#### todo-ex-sample の依存

Initializr では最小構成で生成し、Yavi / Flyway / Testcontainers などは生成後に手動で `build.gradle` に追記する想定（DDD/オニオン/関数型方針が固まってから確定）。

### 生成後のクリーンアップ（マルチプロジェクト化）

Initializr の生成物は単独プロジェクト前提なので、ルートにまとめる際に以下を手動で行う:

1. 各サブの `settings.gradle` を削除（ルート 1 本に統合）
2. 各サブの `gradlew` / `gradlew.bat` / `gradle/wrapper/` を削除（ルート 1 本に統合）
3. 各サブの `build.gradle` から、ルートに引き上げた項目（Java toolchain・`group`・`repositories`・共通テスト依存・`useJUnitPlatform()` 等）を削除
4. 各サブの `build.gradle` の Spring Boot プラグインは **バージョン無しで適用**（バージョンはルートが保持）

### DB 構成

- 1 つの `docker-compose.yml` をルートに配置。
- PostgreSQL コンテナは 1 台。その中に `todo_sample` と `todo_ex_sample` の 2 データベースを作成（初期化 SQL で対応）。
- 接続情報はアプリごとに `application.yml` で分離。

---

## todo-sample 固有方針

- プレゼンテーション: Spring MVC + Thymeleaf
- アーキテクチャ: アプリケーション層 / ドメイン層 / インフラ層の 3 層
- 参考書の構成・命名を優先する。発展的な工夫は持ち込まない（それは todo-ex-sample 側で行う）。
- マイグレーションは参考書の方針に準拠（Flyway を使わないなら使わない）。

---

## todo-ex-sample 固有方針

- REST API（リクエスト/レスポンスはすべて JSON）
- マイグレーション: **Flyway**
- 設計: **オニオンアーキテクチャ + DDD**
  - Domain / Application / Infrastructure / Presentation の 4 層
  - 依存方向は常に Domain に向かう。Domain は他層に依存しない。
- VO: Java `record` + **Yavi** で「生成時に必ず妥当」(always valid) を担保
- **関数型プログラミング**志向（不変、副作用の分離、`Optional` などの活用）
- UT / IT を明確に分離。IT は Testcontainers で実 PostgreSQL を使う。
- テストはレイヤごとの責務に沿って書く（Domain は純粋ロジックの UT、Infrastructure は IT 中心）。

---

## ディレクトリ規約

モノレポ（マイクロサービス的）構成。ルートに共通基盤を置き、サブプロジェクトは固有の依存だけを追加する。

```text
spring-todo-sample/
├── CLAUDE.md
├── compose.yaml                # PostgreSQL 1 コンテナ・2 DB（共通基盤）
├── db/
│   └── init/
│       └── create-database.sql # 初回起動時に 2 DB を作成
├── settings.gradle             # サブプロジェクトを include
├── build.gradle                # 共通プラグイン適用・バージョン管理
├── gradle/wrapper/             # Gradle Wrapper（ルート 1 本）
├── gradlew                     # ルート 1 本
├── gradlew.bat                 # ルート 1 本
├── todo-sample/
│   ├── build.gradle            # todo-sample 固有の依存のみ
│   └── src/...
└── todo-ex-sample/
    ├── build.gradle            # todo-ex-sample 固有の依存のみ
    └── src/...
```

### ルートの責務（`build.gradle` / `settings.gradle`）

- `settings.gradle` で `include 'todo-sample', 'todo-ex-sample'`（マルチプロジェクト構成）
- `plugins { ... }` で Spring Boot プラグインと `io.spring.dependency-management` を **`apply false`** で宣言（バージョンはここで一元管理）
- `subprojects { ... }` で以下を一括適用:
  - `java { toolchain { languageVersion = JavaLanguageVersion.of(21) } }`
  - `group = 'com.example'` / `version`
  - `repositories { mavenCentral() }`
  - `tasks.named('test') { useJUnitPlatform() }`
  - 共通テスト依存（`spring-boot-starter-test`、`junit-platform-launcher`）
- Gradle Wrapper（`gradlew` / `gradle/wrapper/`）はルートに 1 本のみ

### サブプロジェクトの責務（`<project>/build.gradle`）

- `plugins { ... }` で Spring Boot / dependency-management を **バージョン無しで適用**
- `dependencies { ... }` に **固有の依存のみ** を追加
- Java バージョン・`group`・`repositories` などは再宣言しない
- `settings.gradle`・`gradlew`・`gradle/wrapper/` は持たない（ルートで一元管理）

### 共通基盤

- `compose.yaml` はルートに 1 つだけ。両アプリが同じ PostgreSQL コンテナを参照する
- 接続情報はアプリごとに `application.yml` で分離（DB 名のみ差し替え）

---

## 既存ルール（参照のみ）

以下のルールは別ファイルに定義されている。CLAUDE.md ではここで列挙するに留め、内容は再掲しない。これらと矛盾する指示を本ファイルに書かない。

- `.claude/rules/commands.md` — 破壊的操作・特権コマンド等の禁止
- `.claude/rules/security.md` — `.env` ・認証情報の読み込み禁止
- `.claude/rules/coding.md` — 既存規約への追従・スコープ限定・冗長コメント禁止
- `.claude/rules/git.md` — push 禁止・コミットメッセージ日本語・`Co-authored-by` 付与

---

## AI への補足メモ

- 学習が目的なので、Claude が「ベストプラクティスだから」と先回りして実装方針を変えるのは避ける。提示はするが採否はユーザーに委ねる。
- 不明点があれば実装を進める前に質問する。前提を勝手に拡張しない。
- バージョンや依存ライブラリの選定理由は短く添える（写経のときは「参考書がこの構成だから」で十分）。
