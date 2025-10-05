# Issue #35 Dialect疎結合化 調査レポート

## 背景
- Issue #35 "モジュール構成・その他俯瞰検証" では、SeasarBatis の SQL 処理が複数データベース方言（H2/MySQL/Oracle/PostgreSQL/SQL Server など）で一貫して動作することが品質ゲートに含まれています。
- `#file:jdbc` 配下の統合テスト群は各DB向けの `SBJdbcManagerSqlFileIntegration*Test` を用意しており、SQL出力の方言差異を吸収できる仕組みがMUSTであることを示しています。
- 現状は `SBMyBatisSqlProcessor` が静的ユーティリティとしてプレースホルダ埋め込みを担当しており、Dialect拡張の余地が限定的です。本レポートでは現行コードを調査し、疎結合化に向けた対応方針と作業計画を整理します。

## Issue #35 対応範囲
- 本Issueでは、初期対応として以下の Dialect を提供対象とします（1〜3種類に限定）。
   - **OracleDialect**: Oracle Database 19c LTS をターゲットとし、`TO_DATE`/`TO_TIMESTAMP` などの標準関数を活用したフォーマットを提供。
   - **SqlServerDialect**: Microsoft SQL Server 2022 をターゲットとし、`FORMAT`/`CONVERT` による日付・時刻表現を提供。
   - **PostgresDialect**: PostgreSQL 16 系をターゲットとし、H2 は PostgreSQL Mode での運用を前提とする（MySQL 方言はスコープ外）。
- 上記3方言のうち、最低限 Oracle と SQL Server を MUST 対応とし、PostgreSQL/H2 を補完対象とします。
- 追加方言は将来リリースで段階的に拡張する方針とし、本Issueでは Resolver フレームワークと拡張ポイントを整備することを優先します。
- 方言追加状況は別ドキュメント（`docs/dialect-support-matrix.md`）で管理し、Issue クローズ時点の対応状況を記録します（現状 Draft）。

## 現状整理
- SQL最終生成は `lib/src/main/java/jp/vemi/seasarbatis/core/sql/processor/SBMyBatisSqlProcessor.java` の `process` メソッドに集約されています。
  - MyBatis `DynamicSqlSource` を経由し、抽出した `ParameterMapping` ごとに `formatParameter` を呼び出してリテラルへ変換しています。
  - `Dialect` インタフェースは `SBMyBatisSqlProcessor` 内部の `public interface Dialect` としてネスト定義されています。
  - Dialect 解決は `ServiceLoader.load(Dialect.class)` で最初に見つかった実装1件を返すだけで、DBごとの切替フックは存在しません。
  - `META-INF/services` の提供クラス名は `SBMyBatisSqlProcessor$Dialect` となるため、実装側からの提供が煩雑かつ分かりにくい構造です。
- `SBSqlProcessor` や `SBQueryExecutor` など上位層は Dialect を意識せず、MyBatis `Configuration` だけを受け取る構造になっています。
- 文字列・日付・コレクションなど主要型のフォーマットは `formatParameter` 内に直書きされており、方言別の拡張ポイントは `escapeString` / `formatDate` / `formatLocalDate` / `formatLocalDateTime` の4メソッドに限定されています。
  - Oracle 向けの `TO_DATE` / `TO_TIMESTAMP` などを利用したい場合でも、現在のAPIでは文字列返却しかできないため、変換ロジックの再利用が困難です。

## 課題・問題点
- **ネストインタフェースの制約**: `SBMyBatisSqlProcessor$Dialect` という名前での ServiceLoader 登録が必要になり、実装拡張の障壁が高い。
- **DB毎の切替不可**: Configuration や DataSource からデータベースIDを判定する手段が無く、複数DBを同時利用するアプリケーションで Dialect を使い分けられない。
- **テスタビリティの不足**: `SBMyBatisSqlProcessor` が static メソッド主体であり、モック差し替えによるユニットテストが困難。
- **フォールバックの曖昧さ**: Dialect 未提供時にデフォルト実装へフォールバックする仕様が曖昧で、Issue #35 の品質ゲートを満たすには挙動の明文化と検証が必要。

## 対応方針（案）
1. **Dialect インタフェースのトップレベル化**
   - 新規パッケージ `jp.vemi.seasarbatis.core.sql.dialect` に `SBDialect`（仮称）を定義し、文字列エスケープや日付フォーマット、将来の拡張を見据えたAPIを設計。
   - 既存APIで露出している 4 メソッドに加え、数値・ブーリアン・配列/コレクションの処理委譲や、任意リテラルを直接返却できる `formatLiteral` などのフックを検討。
2. **Dialect 解決器の導入**
   - `SBDialectResolver`（仮称）を新設し、MyBatis `Configuration#getDatabaseId()` or `Environment`、もしくは DataSource の JDBC URL から方言を判定。
   - Resolver は `ServiceLoader` で複数件ロードし、`supports(String databaseId, DataSource dataSource)` などの条件一致で最適な Dialect を選択。複数候補がある場合の優先順位ルールを Issue #35 の品質ゲートに明記。
3. **SBMyBatisSqlProcessor のインスタンス化**
   - `SBSqlProcessor` から Dialect Resolver を注入し、`process` 実行時に対象 Dialect を取得するようリファクタリング。
   - `formatParameter` を非static に切り出し、Dialect ごとの処理委譲をシンプル化。既存の型変換ロジックは Default Dialect へ移譲。
4. **デフォルト実装の整備**
   - OracleDialect / SqlServerDialect / PostgresDialect のベース実装を用意し、各DB LTSバージョンに合わせたリテラル生成・エスケープを提供。
   - PostgresDialect は H2(PostgreSQL Mode) と PostgreSQL 本体の双方で再利用できるAPIを整備し、MySQL系はフォローしない。
5. **設定エントリポイントの提供**
   - アプリケーション利用者が Dialect を明示指定できるよう、`SBSqlProcessorBuilder` もしくは `SBQueryExecutor` のファクトリに setter を追加。
   - `application.properties` 等から外部設定できる拡張ポイントも Issue #35 で議論。
6. **ドキュメントとサンプル**
   - `docs/SQL-PARSER-DESIGN.md` に Dialect 拡張手順を追記。
   - `lib/src/test/java/...` に Dialect 単体テストと統合テストを追加し、`SBJdbcManagerSqlFileIntegration*Test` の期待値差分を最小化。

## Dialect別フォーマット規約（Draft）
- 共通方針
   - 文字列: SQLインジェクション対策として各方言専用のエスケープ関数を利用する。`null` はリテラル `NULL` を使用。
   - 数値/ビッグデシマル: 文字列化せずリテラル出力（科学的記数法は使用しない）。
   - ブール: 方言毎のBooleanリテラル（`TRUE/FALSE`, `1/0`, `ON/OFF` 等）を使用。
   - コレクション/配列: `IN (...)` 展開は方言共通。リストが空の場合は `1=0` を返すポリシーを導入。
   - タイムゾーン: `OffsetDateTime`/`ZonedDateTime`/`Instant` をサポート対象に含め、UTC へ正規化後に方言仕様で整形。`LocalDateTime` はタイムゾーン無しのローカル時刻として扱う。

### PostgresDialect（PostgreSQL 16 / H2 PostgreSQL Mode）
- 文字列: `E'...'` 表記と `replace("'", "''")` を併用し、`standard_conforming_strings` を前提とした安全なリテラルを生成。
- 日付型 (`LocalDate`/`java.sql.Date`): `DATE 'YYYY-MM-DD'` 形式。
- 時刻型 (`LocalTime`/`java.sql.Time`): `TIME 'HH24:MI:SS.US'` 形式。
- タイムスタンプ (`LocalDateTime`/`Timestamp`): `TIMESTAMP 'YYYY-MM-DD HH24:MI:SS.US'` 形式。
- タイムゾーン付 (`OffsetDateTime`/`Instant`): `TIMESTAMPTZ 'YYYY-MM-DD HH24:MI:SS.US+TZ'` 形式。
- ブール: `TRUE`/`FALSE`。
- JSON/Binary など拡張型は本Issue範囲外。

### OracleDialect（Oracle Database 19c）
- 文字列: `q'[ ... ]'` を利用（特殊文字含む場合）し、基本は `'...'` + `''` エスケープ。
- 日付: `TO_DATE('YYYY-MM-DD','YYYY-MM-DD')`。
- 時刻: `TO_DATE('YYYY-MM-DD HH24:MI:SS','YYYY-MM-DD HH24:MI:SS')` を `DATE` 変換として扱う。
- タイムスタンプ: `TO_TIMESTAMP('YYYY-MM-DD HH24:MI:SS.FF6','YYYY-MM-DD HH24:MI:SS.FF6')`。
- タイムゾーン付: `TO_TIMESTAMP_TZ('YYYY-MM-DD HH24:MI:SS.FF6 TZH:TZM','YYYY-MM-DD HH24:MI:SS.FF6 TZH:TZM')`。UTC 正規化後に `TZH:TZM` を生成。
- ブール: Oracle では NUMBER(1) を前提とし `1`/`0` を使用。Boolean 列サポートは将来対応とし、`VARCHAR2` で `TRUE`/`FALSE` を利用する運用例をガイドに記載。

### SqlServerDialect（SQL Server 2022）
- 文字列: `N'...'` を使用し、`''` エスケープ。UTF-16 ベースを前提。
- 日付: `CONVERT(date,'YYYY-MM-DD',23)` を使用。
- 時刻: `CONVERT(time(7),'HH:mm:ss.ffffff',21)` を使用。
- タイムスタンプ: `CONVERT(datetime2(6),'YYYY-MM-DD HH:mm:ss.ffffff',21)`。
- タイムゾーン付 (`OffsetDateTime`/`Instant`): `SWITCHOFFSET(CONVERT(datetimeoffset(7),'YYYY-MM-DD HH:mm:ss.ffffff +TZ',21), '+TZ')` 形式。UTC 正規化後に `+TZ` を組み立て。
- ブール: `1`/`0`。`bit` 型へのマッピングを想定。

- それぞれの Dialect 実装では上記フォーマット規約をユーティリティ化し、ユニットテストでフォーマット結果を固定化する。
- 規約は Draft とし、Issue #35 で検証しながら確定版を `SQL-PARSER-DESIGN.md` に反映する。

## 作業計画
1. **設計フェーズ**（Issue #35 に設計コメントを投稿）
   - API 仕様ドラフトとクラス図（簡易）を共有。
   - Dialect Resolver の優先順位、フォールバックルール、例外時の扱い（例: 不明なDB）の決定。
2. **実装フェーズ 第1弾**
   - `core/sql/dialect` パッケージの追加、`SBMyBatisSqlProcessor` のインスタンス化対応、Resolver とキャッシュ機構の導入。
   - PostgresDialect の実装と単体テストを整備し、H2(PostgreSQL Mode) 統合テストを更新。
3. **実装フェーズ 第2弾**
   - OracleDialect の実装と単体テストを追加し、Oracle Database 19c 向けの Testcontainers ジョブ（任意実行）を整備。
   - `SBJdbcManagerSqlFileIntegrationOracleTest` の期待値を新Dialect仕様に合わせて調整。
   - 旧 ServiceLoader 実装との互換ラッパー (`SBDialectAdapter`) を実装し、非推奨アナウンスを追加。
4. **実装フェーズ 第3弾**
   - SqlServerDialect の実装と単体テストを追加し、SQL Server 2022 Testcontainers ジョブ（任意実行）を整備。
   - `SBJdbcManagerSqlFileIntegrationSqlServerTest` の期待値を新Dialect仕様に合わせて調整。
5. **仕上げフェーズ**
   - ドキュメント更新 (`SQL-PARSER-DESIGN.md`, CHANGELOG/UPDATE.md 等)。
   - リリースノート草案と導入ガイド追記。
   - Issue #35 での最終レビュー、必要に応じてフォロワップIssue作成。
   - `docs/UPDATE.md` にローカル検証手順と Testcontainers 実行方法、WARN ログポリシーを追記。
   - `docs/dialect-support-matrix.md` を更新し、サポート状態を公開。

## リスク・懸念点と対応方針
- Oracle / SQL Server 方言はDATE/TIMESTAMP変換や識別子クォートの扱いが複雑で、開発環境に実データベースが必要となる可能性。
   - **対応方針**: Testcontainers を活用した最小構成の統合テストを整備し、CI ではスキップ可能なオプション扱いとする。ローカル検証手順を `docs/UPDATE.md` に追記。
- 既存プロジェクトで独自に `ServiceLoader` ベースの Dialect を導入しているケースがあれば破壊的変更となるため、互換性ポリシーの整理が必要。
   - **対応方針**: 旧 API を非推奨化しつつ 1 リリース分はラッパーで移行互換を維持。`SBDialectAdapter` を提供し、既存 ServiceLoader 実装を自動登録するブリッジを実装。
- `Configuration#getDatabaseId()` が未設定の場合の判定ロジック（JDBC URLパース）にはコストがかかるため、キャッシュや遅延解決の設計が求められる。
   - **対応方針**: Resolver に LRU キャッシュを組み込み、初回判定結果を Configuration 単位で保持。判定失敗時はデフォルトDialectへフォールバックし、WARNログで利用者へ通知。

## 補足
- 本調査ではコード変更およびテスト実行は行っていません。
- 作業進行時は Issue #35 へ進捗と設計判断を都度共有する方針です。
