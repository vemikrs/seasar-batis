# SQL Parser Design Note

## 1. 概要
- SeasarBatis の SQL パーサーは S2JDBC 互換コメントを解析し、MyBatis 実行可能な SQL へ変換します。
- 2025-10 リファクタリングで Tokenizer → AST → Renderer 構成へ刷新し、ネスト条件・多行ブロック・コレクション展開・デフォルトリテラル保持を実現しました。
- 解析段階で得たパラメータは `ParsedSql.parameterValues` に保持され、MyBatis 側での値展開やログ出力に再利用されます。

## 2. 処理フロー
1. **Parser**: SQL 内の `/*COMMENT*/` を検出して `TextNode` / `PlaceholderNode` / `BeginNode` / `IfNode` へ分解。`captureDefaultLiteral` によりコメント直後のダミー値を取り出して保持します。
2. **Renderer**: AST を走査し、条件判定 (`IfNode`)・ブロック抑制 (`BeginNode`)・パラメータ展開 (`PlaceholderNode`) を実施。コレクション型は `param_0`, `param_1` のように連番で展開し、既定値が存在する場合はフォールバック。
3. **SBSqlProcessor**: `ParsedSql` で収集したパラメータマップを MyBatis `DynamicSqlSource` に渡し、`SBMyBatisSqlProcessor` が実値を挿入した最終 SQL を生成します。

## 3. サポートするコメント構文
- `/*BEGIN*/` `/*END*/`: ネスト/複数行対応、空 WHERE 句（`WHERE 1=1`）は自動除去。
- `/*IF expression*/`: `AND`/`OR` の優先順位、括弧、比較演算（`==`, `!=`, `<`, `<=`, `>`, `>=`）、`IS [NOT] NULL`、文字列/数値/日付の型変換に対応。
- `/*param*/default`: 単一値は `#{param}`、コレクション/配列は `#{param_0}, #{param_1}, ...` を `( )` でラップ。パラメータ未指定時は `default` を残す。
- `LIKE` パターン: `/*keyword*/'%'` のようなダミー値はそのまま残り、プレースホルダのみ置換されます。

## 4. テスト整備
- `SBSqlParserTest`: ネスト IF、多行 BEGIN、デフォルトリテラル、NULL 判定、コレクション展開などのユニットテスト。
- `SBSqlParserFileCompatibilityTest`: `sql/complex-users-query.sql` を用いたファイル互換テスト。解析結果と `SBSqlProcessor` 出力を比較。
- `SBJdbcManagerSqlFileIntegrationTest`: H2（インメモリ）と MySQL（Testcontainers）へ同一 SQL ファイルを投入し、結果件数を検証。
- `SBSqlParserPerformanceTest`: 同一 SQL を 500 回解析して 2 秒以内に完了することを確認する軽量パフォーマンステスト（`@Tag("performance")`）。

## 5. 品質ゲート
- すべてのユニットテスト・統合テスト・性能テストが成功すること。
- 代表的な SQL セット（`sql/complex-users-query.sql` など）の解析結果が Issue #35 へ記録された期待 SQL と一致すること。
- MySQL/H2 双方で `selectBySqlFile` が例外なく実行できること。
- パーサー変更時は `ParsedSql.parameterValues` の互換性に注意し、`SBMyBatisSqlProcessor` とのシグネチャを維持すること。

## 6. 既知の制限・今後の課題
- `/*ELSE*/` ブロック、`/*FOR*/`/`/*FOREACH*/` 構文、EL 式や SpEL 互換の条件式は未実装。
- 条件式における関数呼び出し、`BETWEEN`、`IN` のネストしたサブクエリなどは明示的にサポートしていません。
- AST キャッシュは未導入のため、同一 SQL を高頻度で解析する場合は `SBSqlFileLoader` 側でキャッシュすることを推奨します（将来改善予定）。

## 7. 参考リソース
- Issue #35 "モジュール構成・その他俯瞰検証"：進捗コメントと品質ゲートの記録。
- `docs/architecture-review-20251005.md`：アーキテクトレビューとリスク分析の詳細。
- `README.md`：最新の制限事項と利用ガイド。
