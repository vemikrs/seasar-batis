## リポジトリ基礎情報
- このリポジトリはSeasar2ライクなMyBatis実装を提供するプロジェクトです。
- GradleベースのJavaライブラリプロジェクトです。
- Gradleのバージョンは8.9を使用しています。

## このプロジェクトについて
- プロジェクト名は「SeasarBatis」です。
- このプロジェクトは、Seasar2のS2TIGERプロジェクトのインタフェースや仕様を参考にしていますが、Seasar2プロジェクトとは無関係です。

## ルール
- 基本パッケージ名は「jp.vemi.seasarbatis」を使用します。
- 各種設定や依存関係は、以下の複数の`build.gradle`ファイルに記述します。
    - `build.gradle` (ルートプロジェクト)
    - `lib/build.gradle` (コアモジュール)
    - `spring/build.gradle` (Spring統合モジュール)
