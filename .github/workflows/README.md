# GitHub Workflows for SeasarBatis

このディレクトリには、SeasarBatisプロジェクトのためのGitHub Actionsワークフローが含まれています。

## ワークフロー一覧

### 1. Comprehensive Test Suite (`comprehensive-tests.yml`)

**目的**: プロジェクト全体の包括的なテストスイートを実行する汎用的なワークフロー

**トリガー**:
- 手動実行 (workflow_dispatch) - PR受入時などに任意で実行可能
- mainブランチへのpush
- mainブランチへのPR

**特徴**:
- 複数のJavaバージョン (17, 21) でのテスト
- 柔軟なテスト範囲選択 (全て / 単体テストのみ / 統合テストのみ)
- データベース互換性テスト (H2, MySQL)
- コードカバレッジレポート生成
- ビルドとパッケージング検証
- 包括的なテストレポート生成

**手動実行時の設定項目**:
- `test_scope`: テスト範囲 (all/unit-only/integration-only)
- `coverage_report`: カバレッジレポート生成の有無

**使用方法**:
```bash
# GitHub UIから手動実行
# Actions タブ → Comprehensive Test Suite → Run workflow

# または、PR受入時に自動実行される
```

### 2. Quick CI Tests (`ci.yml`)

**目的**: 開発中の素早い品質チェック

**トリガー**:
- 全ブランチへのpush (mainブランチ除く)
- 全ブランチへのPR

**特徴**:
- 高速な実行
- 基本的な単体テストと統合テスト
- Java 17での実行

## 実行環境

### 必要な環境
- Java 17 以上
- Gradle 8.9
- MySQL 8.0 (統合テスト用)
- H2 Database (単体テスト用)

### 使用されるサービス
- **MySQL**: 統合テストでのデータベース操作テスト
- **TestContainers**: コンテナベースのテスト環境

## アーティファクト

各ワークフローは以下のアーティファクトを生成します:

### Comprehensive Test Suite
- `test-results-java-{version}`: テスト実行結果
- `coverage-reports-java-{version}`: カバレッジレポート
- `build-artifacts`: ビルド生成物
- `comprehensive-test-report`: 最終テストレポート

### Quick CI Tests
- `quick-test-results`: 基本テスト結果

## 設定詳細

### データベース設定
```yaml
services:
  mysql:
    image: mysql:8.0
    env:
      MYSQL_ROOT_PASSWORD: test
      MYSQL_DATABASE: seasarbatis_test
      MYSQL_USER: test
      MYSQL_PASSWORD: test
```

### 環境変数
```yaml
env:
  SPRING_PROFILES_ACTIVE: test
  MYSQL_URL: jdbc:mysql://localhost:3306/seasarbatis_test
  MYSQL_USERNAME: test
  MYSQL_PASSWORD: test
```

## カスタマイズ

### テスト範囲の変更
`comprehensive-tests.yml`の`test_scope`入力パラメータで制御:
- `all`: 全テスト実行 (デフォルト)
- `unit-only`: 単体テストのみ
- `integration-only`: 統合テストのみ

### Javaバージョンの追加
`matrix.java`配列にバージョンを追加:
```yaml
strategy:
  matrix:
    java: [17, 21, 22]  # 22を追加
```

### データベースの追加
`database-compatibility-test`ジョブの`matrix.database`に追加:
```yaml
strategy:
  matrix:
    database:
      - h2
      - mysql
      - postgresql  # 追加
```

## トラブルシューティング

### よくある問題

1. **Gradleの権限エラー**
   - `chmod +x gradlew`が実行されているか確認

2. **データベース接続エラー**
   - MySQLサービスのヘルスチェックが通っているか確認
   - 環境変数が正しく設定されているか確認

3. **テストタイムアウト**
   - TestContainersの起動時間を考慮してタイムアウトを調整

4. **メモリ不足**
   - Gradleのメモリ設定を調整
   - 並列実行数を制限

### ログの確認方法
1. GitHub Actions画面でワークフロー実行を選択
2. 失敗したジョブをクリック
3. 各ステップのログを確認
4. アーティファクトをダウンロードして詳細を確認

## 開発者向けガイド

### ローカルでの実行
```bash
# 全テスト実行
./gradlew clean test

# カバレッジレポート生成
./gradlew jacocoTestReport

# 特定モジュールのテスト
./gradlew :lib:test
./gradlew :spring:test
```

### 新しいテストの追加
1. 適切なモジュール(`lib`または`spring`)にテストクラスを追加
2. テスト名は`*Test.java`または`*Tests.java`とする
3. JUnit 5 (Jupiter) を使用
4. 必要に応じて`@TestContainer`アノテーションを使用

## 今後の拡張予定

- パフォーマンステストの追加
- セキュリティスキャンの統合
- 複数OS (Windows, macOS) での動作確認
- Dockerイメージのビルドとテスト
- 自動デプロイメント機能