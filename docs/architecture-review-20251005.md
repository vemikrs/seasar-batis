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
