# SeasarBatis アーキテクト・構成レビュー（2025-10-05）

## 1. ライブラリ概要
- `README.md` および `lib/src/main/java` の構成から、SeasarBatis は Seasar2 の JdbcManager ライクな操作性を MyBatis 上で再現することを目的とした Java 21 向けライブラリであり、`SBJdbcManager` を中心に CRUD・排他制御・SQL ダイナミクス・トランザクション管理を一体提供しています。
- コアは `lib` モジュール（バージョン `1.0.0-beta.2`）で、クエリビルダー（`core/builder`）、エンティティメタ（`core/entity`）、楽観的排他（`core/config` と `core/entity`）、トランザクション（`core/transaction`）、S2JDBC 互換 SQL 解析（`core/sql`）などへ責務分割されています。
- `spring` モジュール（バージョン `1.0.0`）は Spring Boot 3 系統合を目的に `SpringJdbcManager` とオートコンフィグクラスを提供し、`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` で自動登録する構造です。

## 2. クラス構成の所見
- **責務分割の明瞭さ**: `jp.vemi.seasarbatis.core` 配下がビルダー・クエリ・エンティティ操作・SQL 解析に整理され、`jp.vemi.seasarbatis.jdbc.SBJdbcManager` がそれらをオーケストレーションする構成は理解しやすく、トランザクションや楽観ロック (`core/entity/SBOptimisticLockSupport`) の切り出しも適切です。
- **SQL 解析ロジックのリスク**: `lib/src/main/java/jp/vemi/seasarbatis/core/sql/processor/SBSqlParser.java` の `processIfConditions` は 1 行単位でしか条件ブロックを保持できず、ネストした `/*IF ...*/` の `/*END*/` でスタックを正しく巻き戻さないため、多行ブロックや複合条件を含む S2JDBC 形式 SQL が崩れる危険があります。リリース前にロジックの再設計と網羅テストが必要です。
- **トランザクション層**: `lib/src/main/java/jp/vemi/seasarbatis/core/transaction/SBTransactionManager.java` は `SBThreadLocalDataSource` で `SqlSessionFactory` の `Environment` を包み替える高度な制御を行っており、独立トランザクション（`executeWithTransaction(true, ...)`）も提供しています。一方でクラス先頭にプロジェクト標準の著作権ヘッダーが入っておらず、公開時の品質基準に揃える必要があります。
- **Spring 連携**: `spring/src/main/java/jp/vemi/seasarbatis/spring/manager/SpringJdbcManager.java` は `@Transactional` 付与のみでほぼ継承実装となっています。軽量ですが、今後の拡張（例: Spring Data 連携）を視野にファサード層の振る舞いを整理する余地があります。

## 3. モジュール・ライブラリ構成の所見
- **モジュール分割**: コアと Spring 統合を別モジュールに分離している点は Maven Central 公開や BOM 化を考えると妥当です。ただし `lib` が beta 版 (`1.0.0-beta.2`)、`spring` が GA (`1.0.0`) というバージョン乖離が発生しており、同一リリース列で揃えるのが望ましいです。
- **Spring AutoConfiguration**: `spring/src/main/java/jp/vemi/seasarbatis/spring/config/SeasarBatisAutoConfiguration.java` が `@Configuration` を付与しておらず、Spring Boot 3 のオートコンフィグ実行時に `@Bean` メソッドが拾われません。`@Configuration(proxyBeanMethods = false)` の付与と `spring.factories`/`AutoConfiguration.imports` の整合確認が必須です。
- **マルチ DB への備え**: `lib/src/main/java/jp/vemi/seasarbatis/jdbc/SBJdbcManager.java` および `SBJdbcManagerFactory` は `mybatis-config.xml` を固定読み込みして環境を書き換える設計で、DataSource 以外の方言・TypeHandler を切り替える拡張ポイントが限定的です。複数 DB に進む前に、ベンダー別設定注入や SQL 方言差異を扱うフックを用意する必要があります。
- **Spring モジュール依存**: `spring/build.gradle.kts` は `spring-boot-autoconfigure`、`spring-jdbc` などを API として公開しており、利用側の Spring Boot 依存性と衝突する可能性があるため "provided"（Gradle では `compileOnly` + optional）への変更を検討してください。

## 4. テスト・品質の所見
- **存在するテスト**: `SBTransactionManagerDatabaseTest`（H2/MySQL互換モードでの実 DB トランザクション検証）、`SBJdbcManagerBatchTest`（バッチ API の動作確認）、`SBSqlParserTest`（S2JDBC 形式 SQL コメントの変換検証）など、主要機能の単体テストはおおむね整備されています。
- **SQL 解析テストのギャップ**: `lib/src/test/java/jp/vemi/seasarbatis/sql/builder/SBSqlParserTest.java` は 1 行単位の条件や簡易的な `/*BEGIN*/`/`/*IF*/` のみを対象としており、ネスト・多行・`/*ELSE*/` 相当（未実装）といった複雑パターン、`AND`/`OR` 混在条件の評価などが欠落しています。`.sql` ファイル（`test-query.sql`）も複雑度が限定的です。
- **マルチ DB の試験不足**: `lib` 依存には Testcontainers MySQL が含まれるものの、該当アノテーションやコンテナ起動テストは未導入で、H2（MySQL モード）に依存した試験に留まっています。複数 DB をターゲットにする前提なら、MySQL 実機・PostgreSQL 等でのスモーク試験を追加するのが望ましいです。
- **Spring モジュールのテスト**: `spring/src/test/java` 配下には現状テストが存在しないため、AutoConfiguration が Bean を供給できるか、`SpringJdbcManager` が `@Transactional` を正しく伝播するかを検証する統合テストを整備してください。

## 5. 推奨アクション
1. `SBSqlParser` の `processIfConditions`/`processBindVariables` を全面的に見直し、ネスト・多行対応を確実にしたうえで、複雑な `.sql` サンプルを追加して回帰テストを拡充する。
2. `SeasarBatisAutoConfiguration` に `@Configuration(proxyBeanMethods = false)` を付与し、Spring Boot 3 での自動構成が確実に働くことを `spring` モジュールのテストで担保する。
3. `SBJdbcManager`/`SBJdbcManagerFactory` に対し、複数 DB（DataSource 毎の `Configuration` 差し替え、方言別 SQL）を切り替えられる拡張ポイントを設計し、併せて MySQL／PostgreSQL 向けの Testcontainers ベース統合テストを用意する。
4. リリース番号・著作権表記などの仕上げ（`SBTransactionManager.java` など）を揃え、`lib`/`spring` のバージョン整合とドキュメントの更新を行う。
5. Spring モジュールの依存公開範囲を精査し、利用側アプリケーションの BOM/Nebula との依存衝突を避けるために `api` から `implementation`/`compileOnly` への整理を検討する。

## 6. SQL 解析リスク詳細と改修計画

### 6.1 現状リスクの詳細分析
- **単行ブロック前提の `processIfConditions`**: `SBSqlParser#processIfConditions` は `/*IF ...*/` の直後 1 行だけを `pendingLine` へ保持する実装になっており、多行 WHERE 句やネストを含むブロックでは 2 行目以降が無条件に破棄されます。`/*END*/` に到達した際も `ifStack` のサイズにかかわらず 1 レベルしか `pop` せず、ネスト構造が壊れたまま次の条件評価へ進むため、S2JDBC 由来の複雑 SQL では文法崩壊が発生します。
- **コメント除去時のダミー値欠落**: `processBindVariables` は `parameters.containsKey(paramName)` が false の場合にコメント区間を完全削除しますが、S2JDBC の仕様では `/*param*/defaultValue` の default 部分が残るべきです。現実の `.sql` では `/*status*/'ACTIVE'` のような書き方が多く、現行実装では文字列リテラルごとドロップされて無効な SQL が生成されます。
- **条件評価の互換性不足**: `evaluateSingleCondition` は `AND`/`OR` を単純 `split` するだけで括弧や比較演算子の組み合わせを解釈できず、`status != null AND status != "DELETED"` のようなケースで例外化します。`null` 判定以外の `!null` 演算子も実装が S2JDBC と異なるため、表現力が大きく制限されています。
- **ログ出力による性能懸念**: `logger.trace` が各行で呼ばれており、大規模 SQL ファイル読み込み時に `TRACE` レベルが有効だと極端に遅くなります。パーサ改修と同時にロギング粒度の見直しが必要です。

### 6.2 改修ゴール
1. S2JDBC 互換 SQL コメント（`/*IF*/`、`/*BEGIN*/`、`/*END*/`、ダミー値付きプレースホルダ、`IN`/`LIKE` など）を完全サポートする。
2. ネスト、複数行、論理演算、比較演算（`==`, `!=`, `<`, `<=`, `>`, `>=`）および `null` チェックを仕様通りに評価する。
3. パラメータが未提供の場合はダミー値を残し、提供された場合は `#{}` へ正しく置換する。
4. 改修に伴う性能劣化が 1% 未満になるよう、パーサのストリーミング処理とロギングを最適化する。

### 6.3 テストファースト方針
- **ユニットテスト拡充**: `SBSqlParserTest` に以下のケースを追加して先に失敗させる。
  - 多行 `WHERE` 句とネストした `/*IF*/`／`/*BEGIN*/`／`/*END*/`。
  - ダミー値付きプレースホルダの有無（`/*id*/1` など）での置換／非置換。
  - `IN` 句や `LIKE` 句でのコレクション展開（現在は `id IN #{ids}` になり不正）。
  - `AND`/`OR` の混在、括弧付き条件、数値・日付比較。
  - パラメータが存在しない場合にデフォルト値が残ることの確認。
- **SQL ファイルベース検証**: `lib/src/test/resources/sql/` に S2JDBC 既存プロジェクトから引用した複雑 `.sql` を追加し、ファイル読み込み → 解析 → 期待 SQL との突き合わせを行う新テスト（仮称 `SBSqlParserFileCompatibilityTest`）を実装する。
- **回帰テスト**: `SBJdbcManager` 経由で `selectBySqlFile` を実行し、MyBatis の `SqlSession` が例外なく実行できることを H2 と MySQL (Testcontainers) の 2 種で検証する統合テストを追加する。

### 6.4 実装ステップ案
1. **パーサリファクタリング設計**（1.0 人日）
	- Tokenizer → AST → Renderer までの処理フローを整理し、S2JDBC 仕様のサブセットを定義。
	- 既存コードの互換 API (`ParsedSql`) を維持するための移行計画をまとめる。
2. **テスト先行追加**（1.5 人日）
	- 前述のユニット／ファイルテストを `TODO` 付きで投入し、現状の失敗内容を可視化。
3. **`processIfConditions` 再実装**（2.0 人日）
	- 字句解析で `Token(IF)`, `Token(END)`, `Token(TEXT)` を列挙し、スタックでネスト管理。
	- `IF` 条件評価を抽象構文木化し、将来的な `ELSE` 実装余地を残す。
4. **`processBindVariables` 再実装**（1.5 人日）
	- コメント部分とダミー値を別トークンで扱い、未設定時はダミー値を残す。
	- コレクション値の展開（`IN` 句）や `LIKE` 用のエスケープもここで処理できるよう拡張ポイントを用意。
5. **結合テスト／性能確認**（1.0 人日）
	- H2/MySQL Dual での回帰テスト、`TRACE` ログ無効時の性能測定、必要に応じロギング調整。
6. **ドキュメント整備**（0.5 人日）
	- 新仕様を `docs/` 配下（例: `SQL-PARSER-DESIGN.md`）にまとめ、README の既知の制限事項を更新。

### 6.5 リスクと緩和策
- **既存利用者への影響**: 置換ロジックが改善されることで既存利用者の SQL が突然通らなくなる可能性がある。テスト拡充と共に、リリースノートで破壊的変更がないことを示す or フラグで旧挙動にフォールバックできるようにする。
- **実装負荷**: 完全互換を目指すと時間がかかるため、第一段階では SeasarBatis の利用範囲に合わせたサブセット実装とし、実行時に未対応構文を検知して警告を出す。
- **パフォーマンス低下**: Tokenizer/AST 化でオーバーヘッドが増える懸念があるため、必要に応じてキャッシュ戦略（SQL ファイル単位で解析結果をキャッシュ）を導入する。
