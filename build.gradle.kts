plugins {
    // Central Portal 推奨: Vanniktech Maven Publish Plugin を採用
    id("com.vanniktech.maven.publish") version "0.30.0" apply false
}

// Central Portal への公開は各モジュールで本プラグインを適用し設定します。
