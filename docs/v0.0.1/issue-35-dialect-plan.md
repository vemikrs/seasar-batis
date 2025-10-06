# Issue #35 Dialect疎結合化 調査レポート

## 背景
- Issue #35 "モジュール構成・その他俯瞰検証" では、SeasarBatis の SQL 処理が複数データベース方言（H2/MySQL/Oracle/PostgreSQL/SQL Server など）で一貫して動作することが品質ゲートに含まれています。
- `#file:jdbc` 配下の統合テスト群は各DB向けの `SBJdbcManagerSqlFileIntegration*Test` を用意しており、SQL出力の方言差異を吸収できる仕組みがMUSTであることを示しています。
- 現状は `SBMyBatisSqlProcessor` が静的ユーティリティとしてプレースホルダ埋め込みを担当しており、Dialect拡張の余地が限定的です。本レポートでは現行コードを調査し、疎結合化に向けた対応方針と作業計画を整理します。

## Issue #35 対応範囲
- 本Issueでは、**コア機能の極小化とプラガブル機構**を優先し、以下の Dialect を初期提供します(1〜2種類に絞り込み)。
   - **PostgresDialect**: PostgreSQL 17 (最新安定版) および H2 (PostgreSQL Mode) をターゲット。現場ニーズが高い JSONB/ARRAY型を DO 範囲とする。
   - **OracleDialect**: Oracle Database 23ai (最新 LTS) を第一ターゲットとし、`TO_DATE`/`TO_TIMESTAMP` など基本型のみサポート。
- SQL Server は需要確認後にプラグイン実装として別リポジトリ化を検討(コアには含めない)。
- **設計思想**: ServiceLoader を廃止し、DI コンテナ(Spring等)経由で Dialect インスタンスを注入可能にする。MyBatis TypeHandler との共存を前提とした SPI 設計を採用。
- 方言追加状況は `docs/dialect-support-matrix.md` で管理し、運用ポリシー(バージョンアップ・サポート終了基準)も明文化します。

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

## 対応方針(ミニマム・モダン・プラガブル)
1. **Dialect インタフェースの DI 化**
   - `jp.vemi.seasarbatis.core.sql.dialect.SBDialect` を定義し、コンストラクタ注入で `SBSqlProcessor` へ渡す設計。
   - ServiceLoader を排除し、Spring `@Bean` や Guice `Module` から Dialect インスタンスを取得する標準パターンを推奨。
   - API は最小限(`formatString`, `formatDate`, `formatTimestamp`, `formatArray`)に絞り、拡張は `SBDialect` 実装側で完結させる。
2. **MyBatis TypeHandler との役割分離**
   - Dialect は SQL リテラル生成に特化し、ResultSet → Java オブジェクト変換は MyBatis 標準 TypeHandler に委譲。
   - PostgreSQL JSONB 等の拡張型も TypeHandler で吸収し、Dialect は文字列エスケープのみ担当。
3. **DO/DON'T の明文化**
   - **DO**: 基本型(文字列/数値/日付/タイムスタンプ/配列)、PostgreSQL JSONB/ARRAY、Oracle 23ai DATE/TIMESTAMP。
   - **DON'T**: 関数呼び出し(`UPPER`, `CONCAT` 等)、DDL(`CREATE TABLE` 等)、ストアドプロシージャ、XML/Binary 型、DB固有の複雑型(Oracle OBJECT型等)。
   - DON'T 範囲は利用者が MyBatis XML Mapper や Native Query で直接記述する方針とし、Dialect 拡張には含めない。
4. **プラグイン分離**
   - コアリポジトリには PostgreSQL/Oracle のみ含め、SQL Server/MySQL 等は `seasar-batis-dialect-xxx` として別リポジトリ化。
   - Dialect SPI(`DialectProvider`)を定義し、外部実装をクラスパスから自動検出できる軽量機構を提供。
5. **外部設定サポート**
   - `SBDialectConfig` クラスで明示的に Dialect を指定可能にし、`application.yml` 等からプロパティ注入できる Spring Boot Starter を提供。
   - 設定例: `seasarbatis.dialect=postgres17` でバージョン固定、未指定時は JDBC URL から自動判定(キャッシュ有効)。
6. **ドキュメントとサンプル**
   - `docs/DIALECT-PLUGIN-GUIDE.md` に外部 Dialect 開発手順を明記。
   - `examples/` ディレクトリに Spring Boot + PostgreSQL/Oracle の動作サンプルを追加。

## Dialect別フォーマット規約（Draft）
- 共通方針
   - 文字列: SQLインジェクション対策として各方言専用のエスケープ関数を利用する。`null` はリテラル `NULL` を使用。
   - 数値/ビッグデシマル: 文字列化せずリテラル出力（科学的記数法は使用しない）。
   - ブール: 方言毎のBooleanリテラル（`TRUE/FALSE`, `1/0`, `ON/OFF` 等）を使用。
   - コレクション/配列: `IN (...)` 展開は方言共通。リストが空の場合は `1=0` を返すポリシーを導入。
   - タイムゾーン: `OffsetDateTime`/`ZonedDateTime`/`Instant` をサポート対象に含め、UTC へ正規化後に方言仕様で整形。`LocalDateTime` はタイムゾーン無しのローカル時刻として扱う。

### PostgresDialect(PostgreSQL 17 / H2 PostgreSQL Mode)
- **対象バージョン**: PostgreSQL 17.x(最新安定版)、H2 2.3.x(PostgreSQL Mode)
- **DO 範囲**:
  - 文字列: `E'...'` + `''` エスケープ(`standard_conforming_strings=on` 前提)。
  - 日付/時刻: `DATE '...'`, `TIME '...'`, `TIMESTAMP '...'`, `TIMESTAMPTZ '...'` 形式。
  - 配列: `ARRAY[...]` 構文で展開(`IN` 句は `= ANY(ARRAY[...])` へ変換)。
  - JSONB: `'...'::jsonb` キャストで文字列を JSONB リテラル化(TypeHandler で Jackson 連携)。
  - ブール: `TRUE`/`FALSE`。
- **DON'T 範囲**: `jsonb_set`, `array_agg` 等の関数、Composite 型、Range 型、Full Text Search 構文。

### OracleDialect(Oracle Database 23ai)
- **対象バージョン**: Oracle 23ai(最新 LTS)、23c 互換モード
- **DO 範囲**:
  - 文字列: `q'[ ... ]'`(特殊文字含む場合)or `'...'` + `''` エスケープ。
  - 日付: `TO_DATE('YYYY-MM-DD', 'YYYY-MM-DD')`。
  - タイムスタンプ: `TO_TIMESTAMP('YYYY-MM-DD HH24:MI:SS.FF6', 'YYYY-MM-DD HH24:MI:SS.FF6')`。
  - タイムゾーン付: `TO_TIMESTAMP_TZ('YYYY-MM-DD HH24:MI:SS.FF6 +00:00', 'YYYY-MM-DD HH24:MI:SS.FF6 TZH:TZM')`(UTC正規化)。
  - ブール: NUMBER(1) で `1`/`0` 運用(23ai の BOOLEAN 型は将来対応)。
- **DON'T 範囲**: OBJECT 型、VARRAY、Nested Table、XML Type、Spatial/Graph、PL/SQL 構文。

### SqlServerDialect(プラグイン化予定)
- コアには含めず、`seasar-batis-dialect-sqlserver` として別リポジトリで提供予定。
- 対象バージョン: SQL Server 2022、Azure SQL Database。
- DO 範囲: 基本型(文字列/日付/時刻/配列)、DON'T: CLR 型、HierarchyID、XML、JSON(2016+は TypeHandler 推奨)。

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

## 作業計画(ミニマム実装)
1. **設計フェーズ(0.5人日)**
   - DI ベース Dialect API 設計と DO/DON'T 範囲の明文化。
   - MyBatis TypeHandler との責任分離、プラグイン SPI 仕様策定。
2. **実装フェーズ 第1弾(2.0人日)**
   - `SBDialect` インタフェース実装(DI対応、static メソッド廃止)。
   - PostgresDialect(PostgreSQL 17 + JSONB/ARRAY サポート)を実装。
   - H2(PostgreSQL Mode) 統合テストを更新し、JSONB リテラル生成を検証。
3. **実装フェーズ 第2弾(1.5人日)**
   - OracleDialect(Oracle 23ai 対応)を実装。
   - Testcontainers(Oracle Free 23ai)で統合テストを任意実行可能に。
   - 旧 ServiceLoader 除去と移行ガイド作成(破壊的変更として明記)。
4. **プラグイン機構整備(1.0人日)**
   - `DialectProvider` SPI を定義し、外部 Dialect の自動検出機能を実装。
   - Spring Boot Starter(`seasar-batis-spring-boot-starter`)に `SBDialectConfig` 自動構成を追加。
5. **仕上げフェーズ(1.0人日)**
   - `docs/DIALECT-PLUGIN-GUIDE.md` 新規作成(外部 Dialect 開発手順)。
   - `docs/dialect-support-matrix.md` を運用ポリシー込みで更新(バージョンアップ基準・EOL 明記)。
   - `docs/SQL-PARSER-DESIGN.md` に DO/DON'T セクション追加。
   - リリースノート草案と破壊的変更の周知(ServiceLoader 除去)。

## リスク・懸念点と対応方針
- **バージョン選定の妥当性**: PostgreSQL 17 / Oracle 23ai は最新版のため、利用者環境で未対応の可能性。
   - **対応方針**: `dialect-support-matrix.md` に「推奨バージョン」と「検証済みバージョン範囲」を明記。古いバージョン(PG14〜16等)は明示的にサポート外とし、プラグインで対応する方針を周知。
- **ServiceLoader 除去の破壊的変更**: 既存利用者への影響が大きい。
   - **対応方針**: メジャーバージョンアップ(2.0.0)で実施し、1.x系は LTS として6ヶ月間バグフィックスのみ継続。移行ガイド(`MIGRATION-GUIDE-2.0.md`)を整備し、DI パターンへの切り替え手順を詳述。
- **DO/DON'T 範囲の拡張要求**: 利用者から関数サポート等の要望が増える懸念。
   - **対応方針**: Issue テンプレートに「DO/DON'T ポリシー」への同意を明記し、拡張は外部プラグインで行う原則を徹底。コアチームは Issue をクローズし、プラグイン開発ガイドへ誘導。
- **CI コスト増大**: Oracle/PostgreSQL Testcontainers が重い。
   - **対応方針**: デフォルトは H2 のみ実行し、`@Tag("integration-db")` で DB別テストを任意実行化。GitHub Actions では週次スケジュールでフル実行し、PR では H2 のみ通す運用。

## 運用ポリシー(新規追加)
- **バージョン更新基準**: 各 Dialect の対象 DB バージョンは年1回(10月)に見直し。LTS or 最新安定版を優先し、EOL 到達DBは警告後に次リリースでサポート除外。
- **CI 実行ポリシー**: PR マージ時は H2 のみ、main ブランチ push 時は PostgreSQL/Oracle、週次スケジュールで全 DB(プラグイン含む)を実行。
- **プラグイン管理**: 外部 Dialect は `seasar-batis-dialect-xxx` 命名規則を推奨。コアチームは SPI 互換性レビューのみ実施し、プラグイン本体の品質保証は各開発者に委譲。
- **破壊的変更ルール**: メジャーバージョン(X.0.0)でのみ許可。1つ前のメジャーバージョンは6ヶ月間 LTS として維持し、セキュリティ修正のみリリース。

## 補足
- 本調査ではコード変更およびテスト実行は行っていません。
- 作業進行時は Issue #35 へ進捗と設計判断を都度共有する方針です。
- 本計画は Issue #35 「モジュール構成・その他俯瞰検証」の SQL パーサー改修完了後に着手します。
