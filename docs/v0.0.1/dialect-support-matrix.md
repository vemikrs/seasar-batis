# Dialect 対応状況 (Draft)

## 目的
- SeasarBatis がサポートするデータベース方言の一覧と、各Dialectの提供状況・検証状況を記録します。
- Issue #35 時点では **PostgreSQL 17 / Oracle 23ai をコア提供**し、その他は外部プラグイン化を前提とします。
- 本ドキュメントは運用ポリシー(バージョンアップ基準・EOL 基準・CI 実行方針)を含みます。

## 方言一覧
| Dialect 名 | 対象DB (推奨バージョン) | 提供状況 | 検証状況 | 備考 |
|------------|-------------------------|----------|----------|------|
| PostgresDialect | PostgreSQL 17.x / H2 2.3.x (PostgreSQL Mode) | 設計中 (Issue #35) | H2統合テスト更新予定 | JSONB/ARRAY を DO 範囲に含む |
| OracleDialect | Oracle Database 23ai (23c互換) | 設計中 (Issue #35) | Testcontainers (任意) 計画中 | 基本型のみ、OBJECT型等は DON'T |
| SqlServerDialect | SQL Server 2022 / Azure SQL Database | バックログ (プラグイン化) | - | `seasar-batis-dialect-sqlserver` で提供予定 |
| MySQLDialect | MySQL 8.x / MariaDB 11.x | バックログ (プラグイン化) | - | 需要確認後に検討 |
| その他 (DB2, SQLite 等) | - | バックログ | - | 利用者ニーズに応じて評価 |

## 互換マトリクス (Draft)
| DB \ Dialect | PostgresDialect | OracleDialect | SqlServerDialect | MySQLDialect |
|---------------|-----------------|----------------|------------------|--------------|
| PostgreSQL 17 | ✅ (Primary) | - | - | - |
| PostgreSQL 14-16 | ⚠️ (サポート外) | - | - | - |
| H2 (PostgreSQL Mode) | ✅ (Secondary) | - | - | - |
| Oracle 23ai | - | ✅ (Primary) | - | - |
| Oracle 19c | - | ⚠️ (サポート外) | - | - |
| SQL Server 2022 | - | - | 🔌 (Plugin) | - |
| Azure SQL Database | - | - | 🔌 (Plugin) | - |
| MySQL 8.x | - | - | - | 🔌 (Plugin) |
| MariaDB 11.x | - | - | - | 🔌 (Plugin) |

※ ✅: コア提供予定、⚠️: サポート外(プラグイン対応可)、🔌: プラグイン予定。

## 運用ポリシー

### バージョン更新基準
- 各 Dialect の対象 DB バージョンは**年1回(10月)に見直し**を実施します。
- LTS または最新安定版を優先し、EOL 到達 DB は**警告後に次メジャーリリースでサポート除外**します。
- 例: PostgreSQL 12 が 2024年11月 EOL の場合、2025年10月に警告、2026年4月の 3.0.0 でサポート除外。

### サポート終了(EOL)基準
- 各ベンダーの公式 EOL 日を基準とし、**EOL から6ヶ月猶予**を設けます。
- サポート外バージョンでの不具合報告は受け付けず、プラグイン実装での対応を推奨します。
- 検証済みバージョン範囲は各リリースノートに明記し、`dialect-support-matrix.md` と同期します。

### CI 実行ポリシー
- **PR マージ時**: H2(PostgreSQL Mode) のみ実行し、高速フィードバックを優先。
- **main ブランチ push 時**: PostgreSQL 17 / Oracle 23ai を実行(Testcontainers 利用)。
- **週次スケジュール**: 全 DB(プラグイン含む)をフル実行し、結果を GitHub Issue へ自動投稿。

### プラグイン管理方針
- 外部 Dialect は `seasar-batis-dialect-xxx` 命名規則を推奨します。
- コアチームは **SPI 互換性レビューのみ実施**し、プラグイン本体の品質保証は各開発者に委譲します。
- プラグイン一覧は本マトリクスの「プラグインリスト」セクションで管理し、GitHub Topics `seasar-batis-dialect` でも発見可能にします。

## 今後の運用
- 各リリース前に本表を更新し、サポートの追加/変更を明示します。
- Dialect 実装追加時は、本ドキュメントに想定DBバージョン、提供する機能、検証方法を追記します。
- フォーマット規約の詳細は `docs/issue-35-dialect-plan.md` の「Dialect別フォーマット規約(Draft)」を参照し、確定次第 `docs/SQL-PARSER-DESIGN.md` へ反映します。
- 破壊的変更は **メジャーバージョン(X.0.0)でのみ許可**し、1つ前のメジャーは6ヶ月間 LTS として維持します。
