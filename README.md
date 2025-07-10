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
    <version>0.1.0</version>
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
    implementation 'jp.vemi:seasar-batis:0.1.0'
    
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

// エンティティの取得
User user = jdbcManager.findByPk(User.class, 1);

// 一覧取得
List<User> users = jdbcManager.findAll(User.class);

// エンティティの保存
User newUser = new User();
newUser.setName("John");
jdbcManager.insert(newUser);

// エンティティの更新
user.setName("Jane");
jdbcManager.updateByPk(user);

// エンティティの削除
jdbcManager.deleteByPk(User.class, 1);
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

