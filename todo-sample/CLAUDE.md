# todo-sample プロジェクト方針

このファイルは **todo-sample 固有の方針** をまとめる。
モノレポ全体の方針（コラボレーションルール / 共通スタック / ディレクトリ規約 / `.claude/rules/*.md`）は
ルートの [`../CLAUDE.md`](../CLAUDE.md) を参照する。

---

## 位置付け

参考書のハンズオン写経。**素直な実装** を最優先し、発展的な工夫は持ち込まない
（それは todo-ex-sample 側で行う）。

---

## アーキテクチャ

- アプリケーション層 / ドメイン層 / インフラ層の **3 層構成**
- プレゼンテーション: **Spring MVC + Thymeleaf**（REST API は ex 側の責務）

## 命名・構成

- 参考書の構成・命名を優先する
- パッケージは `com.example.todo_sample.<layer>` の素直な配置で OK
- VO / 値オブジェクト化のような発展は行わない（エンティティはクラスで素朴に書く）

## マイグレーション

- 参考書の方針に準拠（Flyway を使わないなら使わない）
- 現状は `schema.sql` / `data.sql` を Spring Boot に読ませる方式

---

## 固有依存（Spring Initializr で選択）

- Spring Boot DevTools
- Lombok
- Spring Web
- MyBatis Framework
- Thymeleaf
- PostgreSQL Driver

`build.gradle` には **固有の依存のみ** を書く。Java バージョン・Spring Boot バージョン・
共通テスト依存などはルートで一元管理しているので再宣言しない（ルート CLAUDE.md の
「ディレクトリ規約」参照）。

---

## DB 接続

- ルートの `compose.yaml` で起動する PostgreSQL コンテナの `todo_sample` データベースに接続する
- 接続情報は `src/main/resources/application.properties` に書く
