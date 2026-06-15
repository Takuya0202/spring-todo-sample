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

これらのバージョン・プラグイン適用はルートの `build.gradle` / `settings.gradle`（必要なら `buildSrc` の convention plugin）で一元管理する。各サブプロジェクトでは Spring Boot や Java バージョンを再宣言せず、固有の依存だけを `dependencies { ... }` に追加する。

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
├── docker-compose.yml          # PostgreSQL 1 コンテナ・2 DB（共通基盤）
├── settings.gradle             # サブプロジェクトを include
├── build.gradle                # 共通プラグイン適用・バージョン管理
├── gradle/                     # wrapper / version catalog（任意）
├── buildSrc/                   # convention plugin を置く場合のみ（任意）
├── todo-sample/
│   └── build.gradle            # todo-sample 固有の依存のみ
└── todo-ex-sample/
    └── build.gradle            # todo-ex-sample 固有の依存のみ
```

- ルートの `settings.gradle` で `include 'todo-sample', 'todo-ex-sample'` する Gradle マルチプロジェクト構成。
- ルート `build.gradle` で Spring Boot プラグイン・Java toolchain（JDK21）・共通テスト依存・MyBatis などを `subprojects { ... }` または convention plugin で一括適用する。
- 各サブプロジェクトの `build.gradle` は **依存追加のみ** を担当（バージョンや共通設定を再宣言しない）。
- `docker-compose.yml` はルートに 1 つだけ。両アプリが同じ PostgreSQL コンテナを参照する。

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
