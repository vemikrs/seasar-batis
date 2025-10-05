# SeasarBatis

[![Maven Central](https://maven-badges.herokuapp.com/maven-central/jp.vemi/seasar-batis/badge.svg)](https://maven-badges.herokuapp.com/maven-central/jp.vemi/seasar-batis)
[![Javadocs](http://javadoc.io/badge/jp.vemi/seasar-batis.svg)](http://javadoc.io/doc/jp.vemi/seasar-batis)

**注意：このライブラリは現在開発中です。不具合や未開発のAPIを含む点に注意してください。**  

---

SeasarBatisは、Seasar2のJdbcManagerのような操作性を提供するMyBatisのラッパーライブラリです。  


## インストール

### Maven
```xml
<dependency>
    <groupId>jp.vemi</groupId>
    <artifactId>seasar-batis</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- 必要な依存関係 -->
<dependency>
    <groupId>org.mybatis</groupId>
    <artifactId>mybatis</artifactId>
    <version>3.5.15</version>
</dependency>
```

### Gradle
```groovy
dependencies {
    implementation 'jp.vemi:seasar-batis:1.0.0'
    
    // 必要な依存関係
    implementation 'org.mybatis:mybatis:3.5.15'
}
```

### スタンドアロンでの使用

```java
SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder()
    .build(Resources.getResourceAsStream("mybatis-config.xml"));
SBJdbcManager jdbcManager = new SBJdbcManager(sqlSessionFactory);
```

## Spring Framework との統合

Spring Frameworkと統合する場合は、以下の追加依存関係が必要です。

### Maven
```xml
<!-- SeasarBatis Spring統合モジュール -->
<dependency>
    <groupId>jp.vemi</groupId>
    <artifactId>seasar-batis-spring</artifactId>
    <version>1.0.0</version>
</dependency>

<!-- Spring統合用の追加依存関係 -->
<dependency>
    <groupId>org.mybatis</groupId>
    <artifactId>mybatis-spring</artifactId>
    <version>3.0.3</version>
</dependency>
<dependency>
    <groupId>org.springframework</groupId>
    <artifactId>spring-jdbc</artifactId>
    <version>6.1.3</version>
</dependency>
```

### Gradle
```groovy
// SeasarBatis Spring統合モジュール
implementation 'jp.vemi:seasar-batis-spring:1.0.0'

// Spring統合用の追加依存関係
implementation 'org.mybatis:mybatis-spring:3.0.3'
implementation 'org.springframework:spring-jdbc:6.1.3'
```

### Spring Boot での設定

`application.yml`に以下の設定を追加します：

```yaml
spring:
  datasource:
    driver-class-name: com.mysql.cj.jdbc.Driver
    url: jdbc:mysql://localhost:3306/your_database
    username: your_username
    password: your_password

mybatis:
  configuration:
    map-underscore-to-camel-case: true
```

Javaコンフィグを追加：

```java
@Configuration
public class SeasarBatisConfig {
    
    @Bean
    public SBJdbcManager sbJdbcManager(SqlSessionFactory sqlSessionFactory) {
        return new SBJdbcManager(sqlSessionFactory);
    }
}
```

## SBJdbcManager の使用方法

### 基本的な使用方法
`SBJdbcManager`はSeasar2の`JdbcManager`に似た操作性を提供するユーティリティクラスです。

```java
@Autowired
private SBJdbcManager jdbcManager;

// エンティティの取得（主キー指定）
User pk = new User();
pk.setId(1L);
User user = jdbcManager.findByPk(pk).getSingleResult();

// 一覧取得
List<User> users = jdbcManager.findAll(User.class);

// エンティティの保存
User newUser = new User();
newUser.setName("John");
jdbcManager.insert(newUser);

// エンティティの更新（主キーを持つエンティティを渡す）
user.setName("Jane");
jdbcManager.update(user);

// エンティティの削除（主キーを持つエンティティを渡す）
User toDelete = new User();
toDelete.setId(1L);
jdbcManager.delete(toDelete);
```

### Seasar2ライクなクエリビルダー
Seasar2のJdbcManagerと同様のスタイルでクエリを構築できます：

```java
// 単一の結果を取得
User user = jdbcManager
    .from(User.class)
    .where()
    .eq("name", "John")
    .getSingleResult();

// 検索結果のリストを取得
List<User> users = jdbcManager
    .from(User.class)
    .where()
    .ge("age", 20)
    .like("name", "J%")
    .orderBy("name ASC")
    .getResultList();

// ComplexWhereとの組み合わせ
List<User> complexUsers = jdbcManager
    .from(User.class)
    .where(jdbcManager
        .complexWhere()
        .add(jdbcManager.where().eq("status", "ACTIVE"))
        .or()
        .add(jdbcManager.where().eq("role", "ADMIN")))
    .orderBy("id DESC")
    .getResultList();
```

### UPDATE操作
WHERE句を使用した更新操作も提供しています：

```java
// 単純な条件での更新
int updatedRows = jdbcManager
    .update(User.class)
    .set("status", "INACTIVE")
    .set("updatedAt", new Date())
    .where(jdbcManager.where()
        .eq("department", "IT")
        .gt("age", 30))
    .execute();

// 複合条件での更新
int updatedRows = jdbcManager
    .update(User.class)
    .set("role", "MANAGER")
    .where(jdbcManager
        .complexWhere()
        .add(jdbcManager.where()
            .eq("status", "ACTIVE")
            .gt("experience", 5))
        .or()
        .add(jdbcManager.where()
            .eq("department", "SALES")
            .ge("performance", 90)))
    .execute();
```

### Batch Operations
SBJdbcManagerは効率的なバッチ処理もサポートしています：

```java
// 複数エンティティの一括登録
List<User> users = Arrays.asList(
    new User("Alice", 25),
    new User("Bob", 30),
    new User("Charlie", 35)
);
List<User> insertedUsers = jdbcManager.batchInsert(users);

// 複数エンティティの一括更新
users.forEach(user -> user.setStatus("ACTIVE"));
List<Integer> updateCounts = jdbcManager.batchUpdate(users);

// 複数エンティティの一括削除
List<Integer> deleteCounts = jdbcManager.batchDelete(users);

// 複数エンティティの一括登録または更新
List<User> mixedUsers = Arrays.asList(
    existingUser,  // 存在する場合は更新
    newUser1,      // 存在しない場合は登録
    newUser2       // 存在しない場合は登録
);
List<User> processedUsers = jdbcManager.batchInsertOrUpdate(mixedUsers);

// 独立したトランザクションでのバッチ処理
List<User> results = jdbcManager.batchInsert(users, true);
```

**バッチ処理の利点：**
- 複数のエンティティを一度のトランザクションで処理するため、パフォーマンスが向上
- トランザクションの境界が明確で、データ整合性が保たれる
- エラー発生時は、バッチ全体がロールバックされる

**使用上の注意：**
- 大量のデータを処理する場合は、メモリ使用量に注意してください
- `isIndependentTransaction`フラグを使用して、既存のトランザクションとの分離レベルを制御できます
- 空のリストやnullを渡すと`SBIllegalStateException`がスローされます

### トランザクション管理
トランザクションをラムダ式で簡単に扱えます：

```java
jdbcManager.transaction(manager -> {
    User user = new User();
    user.setName("John");
    manager.insert(user);

    Address address = new Address();
    address.setUserId(user.getId());
    address.setCity("Tokyo");
    manager.insert(address);
});
```

### SQL直接実行
SQL文を直接実行することもできます：

```java
// SELECT文の実行
Map<String, Object> params = new HashMap<>();
params.put("status", "ACTIVE");
List<User> users = jdbcManager.selectBySql(
    "SELECT * FROM users WHERE status = /*status*/''", 
    params
);

// SQLファイルの実行
List<User> users = jdbcManager.selectBySqlFile(
    "sql/selectUsers.sql", 
    params
);
```

## MyBatis Generator との統合

### プラグインの設定
`generatorConfig.xml`に以下のプラグイン設定を追加してください：

```xml
<plugin type="jp.vemi.seasarbatis.generator.SBEntityMetaPlugin">
    <property name="addSchemaName" value="true"/>
</plugin>
```

### サンプル設定
完全な設定例は`src/test/resources/sample-generatorConfig.xml`を参照してください。

## 国際化（i18n）対応

SeasarBatisは日本語と英語に対応した国際化機能を提供します。

### ロケールの設定

```java
import jp.vemi.seasarbatis.core.i18n.SBLocaleConfig;

// 日本語に設定
SBLocaleConfig.getInstance().setJapanese();

// 英語に設定
SBLocaleConfig.getInstance().setEnglish();

// システムのデフォルトロケールに設定
SBLocaleConfig.getInstance().setDefault();
```

### メッセージの取得

```java
import jp.vemi.seasarbatis.core.i18n.SBMessageManager;

SBMessageManager messageManager = SBMessageManager.getInstance();

// 基本的なメッセージの取得
String message = messageManager.getMessage("transaction.error.execution");

// パラメータ付きメッセージの取得
String paramMessage = messageManager.getMessage("transaction.error.savepoint.not.found", "SP001");
```

### 例外メッセージの国際化

SeasarBatisの例外クラスは自動的に現在のロケールに応じたメッセージを表示します：

```java
// ロケールが日本語の場合：「トランザクション実行エラー」
// ロケールが英語の場合：「Transaction execution error」
throw new SBTransactionException("transaction.error.execution");
```

### 対応メッセージ

- トランザクション関連エラー
- エンティティ操作エラー
- SQL実行エラー
- 一般的なエラーメッセージ

詳細なメッセージ一覧は`src/main/resources/jp/vemi/seasarbatis/messages.properties`と`messages_ja.properties`を参照してください。


## 楽観的排他制御（Optimistic Locking）

SeasarBatis は、バージョンカラムまたは最終更新日時カラムに基づく楽観的排他制御をサポートします。更新時に自動で条件を付与し、競合が検出された場合は `SBOptimisticLockException` をスローします。詳細は `OPTIMISTIC_LOCKING.md` を参照してください。

### 使い方（エンティティ注釈）

```java
@SBTableMeta(name = "users")
public class User {
    @SBColumnMeta(name = "id", primaryKey = true)
    private Long id;

    @SBColumnMeta(name = "name")
    private String name;

    // バージョン方式
    @SBColumnMeta(name = "version", versionColumn = true)
    private Long version;

    // または、最終更新日時方式
    // @SBColumnMeta(name = "updated_at", lastModifiedColumn = true)
    // private LocalDateTime updatedAt;
}
```

### 設定ファイル

`src/main/resources/seasarbatis-optimistic-lock.properties`（デフォルト名）で制御方式を上書きできます。

```properties
seasarbatis.optimistic-lock.enabled=true
seasarbatis.optimistic-lock.default-type=NONE

# エンティティごとの上書き
seasarbatis.optimistic-lock.entity.com.example.User.type=VERSION
seasarbatis.optimistic-lock.entity.com.example.User.column=version

seasarbatis.optimistic-lock.entity.com.example.Order.type=LAST_MODIFIED
seasarbatis.optimistic-lock.entity.com.example.Order.column=updated_at
```

### プログラムによる設定

```java
import jp.vemi.seasarbatis.core.config.SBOptimisticLockConfig;
import jp.vemi.seasarbatis.core.config.SBOptimisticLockConfig.EntityLockConfig;
import jp.vemi.seasarbatis.core.config.SBOptimisticLockConfig.LockType;

SBOptimisticLockConfig config = new SBOptimisticLockConfig()
    .setEnabled(true)
    .setDefaultLockType(LockType.VERSION)
    .addEntityConfig(User.class, new EntityLockConfig(LockType.VERSION, null))
    .addEntityConfig(Order.class, new EntityLockConfig(LockType.LAST_MODIFIED, "updated_at"));

SBJdbcManager jdbcManager = new SBJdbcManager(sqlSessionFactory, config);
```

### 基本的な流れ

```java
User user = jdbcManager.findByPk(pk).getSingleResult();
Long originalVersion = user.getVersion();

user.setName("Updated");
User updated = jdbcManager.update(user);

// VERSION なら自動で +1 される
assert updated.getVersion().equals(originalVersion + 1);
```

### 競合時の例外処理

```java
try {
    User u = jdbcManager.findByPk(pk).getSingleResult();
    u.setName("Updated");
    jdbcManager.update(u);
} catch (SBOptimisticLockException e) {
    // 競合。最新データを取得して再実行など
}
```

より詳しい設計や制約、エッジケースは `OPTIMISTIC_LOCKING.md` を参照してください。

## 既知の制限事項と対応状況

- `/*IF*/`/`/*BEGIN*/`/`/*END*/` を含む S2JDBC スタイルの SQL コメントはネストや複数行に対応しました。コレクション型パラメータは `IN` 句で自動的に個別プレースホルダへ展開され、デフォルトリテラルがそのままフォールバック値として機能します。
- `LIKE` 句ではデフォルトのワイルドカードを保持したまま `#{param}` へ置換されるため、`/*keyword*/'%'` のようなパターンも安全に利用できます。
- SQL ファイル互換性テストに加え、H2 と MySQL/Oracle/PostgreSQL/SQL Server（いずれも Testcontainers）向けの統合テストを追加し、`sql/complex-users-query.sql` をサンプルとして実環境相当の動作確認を行っています。
- 未実装の構文: `/*ELSE*/` ブロック、`FOR`/`FOREACH` 互換記法、Oracle 固有方言の自動最適化などは今後のロードマップに残っています。これらは `docs/SQL-PARSER-DESIGN.md` に整理された設計方針と既知の制約を参照してください。

