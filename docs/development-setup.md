# 開発環境セットアップ (Gradle 9.1.0)

## 前提条件
- OS は Linux / macOS / WSL2 を想定。WSL2 の場合は AF_INET/AF_INET6 ソケット生成を許可したネットワーク設定が必要です。
- Java 21 (Adoptium/Eclipse Temurin 推奨) をインストールし、`JAVA_HOME` と `PATH` を設定します。
- Gradle 9.1.0 を実行できる環境が必要です。`sdkman` などでインストールするか、配布 ZIP を手動取得してください。
- (任意) Docker が利用可能なら `integration` タグ付テストの実行も可能です。

## リポジトリ取得
```bash
git clone git@github.com:vemikrs/seasar-batis.git
cd seasar-batis
```

## Gradle Wrapper の整備
1. `gradle/wrapper/gradle-wrapper.jar` が存在するか確認します。欠損している場合は、グローバル Gradle 9.1.0 で以下を実行します。
   ```bash
   gradle wrapper --gradle-version 9.1.0 --distribution-type bin
   ```
   これにより `gradle-wrapper.jar` と `gradle-wrapper.properties` が生成・更新されます。
2. Wrapper をコミット対象に含め、全員が同じバージョンを利用できるようにします。

## 初回同期とキャッシュ
- インターネット接続が許可されている環境では、以下を一度実行すると Gradle 本体がダウンロードされます。
  ```bash
  GRADLE_USER_HOME="$(pwd)/.gradle" ./gradlew --version
  ```
- オフライン環境で実行する場合は、別環境で取得した `gradle-9.1.0-bin.zip` を `.gradle/wrapper/dists/gradle-9.1.0-bin/<hash>/` 配下に配置してください。`<hash>` は Gradle が生成するランダム文字列 (例: `9agqghryom9wkf8r80qlhnts3`) です。

## 動作確認 (スモーク)
```bash
GRADLE_USER_HOME="$(pwd)/.gradle" ./gradlew :lib:test -DjunitTags=smoke
```
- 成功すると `SBExceptionI18nTest` / `SBSqlParserTest` が実行されます。
- `Could not determine a usable wildcard IP for this machine` が出る場合は、ホスト OS がループバック向けソケットの作成を拒否しています。VPN / セキュリティソフトを確認し、`InetAddress` でワイルドカードバインドが許可されていることを確かめてください。

## よくあるトラブル
- `Unable to access jarfile gradle-wrapper.jar` : Wrapper jar を再生成し、実行権限付きの `gradlew` を利用してください。
- `SocketException: 許可されていない操作です` : ネットワークソケットが禁止されている環境です。WSL2 の `wsl.conf` で `generateResolvConf=false` を設定し、`sudo service network-manager restart` 後に再試行するなど、ソケット作成を許可する必要があります。
- `Could not download ... gradle-9.1.0-bin.zip` : 社内プロキシ下では `gradle.properties` に `systemProp.http.proxyHost` 等を設定し、または事前に ZIP をキャッシュしてから実行してください。

## 次のステップ
- ライブラリ開発: `./gradlew :lib:build` を実行し、基本的なビルドとチェックを回します。
- Spring 統合の検証: `./gradlew :spring:test` をローカルで実行し、Spring Boot オートコンフィグの動作を確認します。
- Docker 環境がある場合は `./gradlew :lib:test -DjunitTags=integration` を実行してマルチ DB 統合テストを実施します。
