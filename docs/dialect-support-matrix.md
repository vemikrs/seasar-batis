# Dialect 対応状況 (Draft)

## 目的
- SeasarBatis がサポートするデータベース方言の一覧と、各Dialectの提供状況・検証状況を記録します。
- Issue #35 時点では初期3方言（Standard, Postgres, SqlServer）にフォーカスし、今後の追加候補を明示します。

## 方言一覧
| Dialect 名 | 想定DB (LTS) | 提供状況 | 検証状況 | 備考 |
|------------|---------------|----------|----------|------|
| PostgresDialect | PostgreSQL 16 / H2 (PostgreSQL Mode) | 設計中 (Issue #35) | H2(PostgreSQL Mode) 統合テスト更新予定 | MyBatis ログ出力をPostgreSQL語感へ統一 |
| OracleDialect | Oracle Database 19c | 設計中 (Issue #35) | Testcontainers (任意) 計画中 | `TO_DATE`/`TO_TIMESTAMP` ベースのリテラル生成 |
| SqlServerDialect | Microsoft SQL Server 2022 | 設計中 (Issue #35) | Testcontainers (任意) 計画中 | `CONVERT`/`FORMAT` ベースのフォーマット |
| StandardDialect | - | バックログ | - | 旧MySQL互換は将来再検討 |
| その他（DB2, SQLite など） | - | バックログ | - | 利用者ニーズに応じて評価 |

## 互換マトリクス (Draft)
| DB \ Dialect | PostgresDialect | OracleDialect | SqlServerDialect |
|---------------|-----------------|----------------|------------------|
| PostgreSQL 16 | ✅ (Primary) | - | - |
| H2 (PostgreSQL Mode) | ✅ (Secondary) | - | - |
| Oracle Database 19c | - | ✅ (Primary) | - |
| SQL Server 2022 | - | - | ✅ (Primary) |
| MySQL / MariaDB | ⚠️ (非対応予定) | - | - |

※ ✅: 対応予定、⚠️: 本Issue範囲外。

## 今後の運用
- 各リリース前に本表を更新し、サポートの追加/変更を明示します。
- Dialect 実装追加時は、本ドキュメントに想定DBバージョン、提供する機能、検証方法を追記します。
- フォーマット規約の詳細は `docs/issue-35-dialect-plan.md` の「Dialect別フォーマット規約（Draft）」を参照し、確定次第 `docs/SQL-PARSER-DESIGN.md` へ反映します。
- 互換ラッパー `SBDialectAdapter` を利用した既存 ServiceLoader 実装の移行状況も、本マトリクスの備考や今後のリリースノートで共有します。
