# BatisFluid

A minimal, modern, pluggable wrapper over MyBatis that brings **fluent, type-safe `JdbcFlow`** and first-class **externalized `.sql`** support.

- Fluent & type-safe query API inspired by S2TIGER's `JdbcManager`
- Externalized SQL (.sql files) and raw SQL execution
- Minimum / Modern / Pluggable

## Installation

### Maven
```xml
<dependency>
  <groupId>jp.vemi</groupId>
  <artifactId>batis-fluid-core</artifactId>
  <version>0.0.2</version>
</dependency>
<dependency>
  <groupId>org.mybatis</groupId>
  <artifactId>mybatis</artifactId>
  <version>3.5.15</version>
</dependency>
```

### Gradle
```groovy
dependencies {
  implementation 'jp.vemi:batis-fluid-core:0.0.2'
  implementation 'org.mybatis:mybatis:3.5.15'
}
```

## Quick Start

```java
var sqlSessionFactory = new SqlSessionFactoryBuilder()
    .build(Resources.getResourceAsStream("mybatis-config.xml"));

var fluid = BatisFluid.of(sqlSessionFactory);

// Fluent query
List<User> users = fluid.jdbcFlow()
    .from(User.class)
    .where(w -> w.eq("status", "ACTIVE").ge("age", 20))
    .orderBy("id DESC")
    .getResultList();

// Externalized SQL
List<User> users2 = fluid.sqlRunner().selectBySqlFile(
    "sql/complex-users-query.sql",
    Map.of("status", "ACTIVE")
);
```

## Spring Boot

Starter: `jp.vemi:batis-fluid-spring` (auto-config `BatisFluidAutoConfiguration`)

```xml
<dependency>
  <groupId>jp.vemi</groupId>
  <artifactId>batis-fluid-spring</artifactId>
  <version>0.0.2</version>
</dependency>
```

Minimal Java config:
```java
@Configuration
public class BatisFluidConfig {
  @Bean
  public JdbcFlow jdbcFlow(SqlSessionFactory factory) {
    return BatisFluid.of(factory).jdbcFlow();
  }
}
```

## Optimistic Locking

- Annotations: `@FluidTable`, `@FluidColumn(versionColumn = true)`
- File overrides: `batisfluid-optimistic-lock.properties`
- Programmatic: `new OptimisticLockConfig().setDefaultLockType(VERSION)`

## Migration

See `MIGRATION GUIDE — v0.0.1 -> v0.0.2`.

## Roadmap

- Spring Boot auto-config improvements
- MyBatis Dynamic SQL interop
- More dialects optimizations and SQL templating
- Remove legacy `SB*` adapters in >= v0.0.3