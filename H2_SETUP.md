# H2 Database Configuration for Local Testing

## 概要

ローカル開発環境でのテスト実行時にMySQLサーバーが不要になるよう、H2データベース設定を追加しました。

## 設定ファイル

### H2テスト設定
- `mybatis-h2-test-config.xml`: H2データベース用のMyBatis設定
- `ddl/01_create_h2_schema.sql`: H2互換のDDLスクリプト
- `ddl/02_insert_h2_data.sql`: H2用のテストデータ

### 使用方法

#### ローカル開発（H2使用）
```bash
# デフォルトでH2を使用
./gradlew test

# 明示的にH2指定
./gradlew test -Dtest.database=h2
```

#### CI/CD環境（MySQL使用）
```bash
# MySQLを使用
./gradlew test -Dtest.database=mysql
```

## 設定の特徴

### H2データベース設定
- **データソース**: UNPOOLED（接続プールなし）
- **データベースファイル**: `/tmp/h2testdb`（一時ファイル）
- **MySQLモード**: H2でMySQLの構文をエミュレート
- **大文字小文字の区別**: 無視（MySQL互換）

### DDLの違い

#### MySQL固有機能のH2での代替
- `ENUM` → `VARCHAR` + `CHECK`制約
- `SET` → `VARCHAR`（カンマ区切り）
- `MEDIUMTEXT` → `TEXT`
- `MEDIUMBLOB` → `BLOB`
- `ENGINE=InnoDB` → 削除

## トラブルシューティング

### よくある問題

1. **データが見つからない**
   - H2のトランザクション分離が原因の可能性
   - ファイルベースH2使用により改善

2. **接続エラー**
   - H2 JARがクラスパスに含まれているか確認
   - `build.gradle`の依存関係を確認

3. **SQL構文エラー**
   - MySQL固有の構文がH2で使用不可
   - DDLスクリプトをH2互換に修正済み

### 今後の改善点

1. **統合テスト**: TestContainersを使用したMySQLテスト
2. **データ永続化**: より確実なH2データ永続化メカニズム
3. **テスト分離**: 単体テストと統合テストの明確な分離

## GitHub Actions統合

### ワークフロー設定
- **ci.yml**: H2を使用した高速テスト
- **comprehensive-tests.yml**: MySQLを使用した完全テスト

環境変数 `test.database` により、実行環境に応じてデータベースを切り替え可能。