# MIGRATION GUIDE — v0.0.1 -> v0.0.2 (BatisFluid)

> Scope: This guide helps you migrate from `SeasarBatis` (v0.0.1) to **BatisFluid** (v0.0.2).
> Goal: Keep builds green via deprecated adapters, then switch fully to the new API.

---

## TL;DR (Checklist)

1. Artifact IDs
   - `jp.vemi:seasar-batis` -> `jp.vemi:batis-fluid-core`
   - `jp.vemi:seasar-batis-spring` -> `jp.vemi:batis-fluid-spring`

2. Package
   - `jp.vemi.seasarbatis` -> `jp.vemi.batisfluid`

3. Entry points
   - `new SBJdbcManager(...)` -> `BatisFluid.of(sqlSessionFactory).jdbcFlow()` or `new JdbcFlow(sqlSessionFactory)`
   - For externalized SQL use `SqlRunner` (old `SBQueryExecutor` etc. remain via deprecated adapters)

4. Annotations and Exceptions
   - Switch to new names; old names remain for now with `@Deprecated`.

5. Config files
   - `seasarbatis-optimistic-lock.properties` -> `batisfluid-optimistic-lock.properties` (old name still accepted)

6. Spring Boot
   - `SeasarBatisAutoConfiguration` -> `BatisFluidAutoConfiguration`

7. Tests and samples
   - Prefer new API; keep a minimal suite against deprecated adapters for a while.

---

## New Core Concepts

- **BatisFluid**: environment/factory
- **JdbcFlow**: fluent, type-safe API inspired by S2TIGER's JdbcManager
- **SqlRunner**: externalized `.sql` and raw SQL execution (S2Dao/S2JDBC spirit)

```java
var fluid = BatisFluid.of(sqlSessionFactory);

// 1) Fluent query
var list = fluid.jdbcFlow()
    .from(User.class)
    .where(w -> w.eq("status", "ACTIVE").ge("age", 20))
    .orderBy("id DESC")
    .getResultList();

// 2) Externalized SQL
var users = fluid.sqlRunner().selectBySqlFile(
    "sql/complex-users-query.sql",
    Map.of("status", "ACTIVE")
);
```

---

## Compatibility Policy (Deprecated)

- `SB*` classes are kept in v0.0.2 with `@Deprecated` and bridge to new ones (inheritance or delegation).
- Plan to remove `SB*` in 1-2 minor releases; target removal: >= v0.0.3 (see CHANGELOG).

---

## Key Renames (excerpt)

See `NAMING_REFACTOR_PLAN.md` for the full table.

### Facade / Management
| old | new |
|---|---|
| `SBJdbcManager` | `JdbcFlow` |
| `SBJdbcManagerFactory` | `BatisFluid` |
| `SBSqlSessionFactory` | `SqlSessionGateway` (optional) |

### Query / Builder
| old | new |
|---|---|
| `SBSelectBuilder` / `SBUpdateBuilder` / `SBDeleteBuilder` | `SelectFlow` / `UpdateFlow` / `DeleteFlow` |
| `SBWhere` / `SimpleWhere` / `ComplexWhere` | `Where` / `SimpleWhere` / `ComplexWhere` |
| `SBOrderByCapable` / `SBWhereCapable` | `OrderByCapable` / `WhereCapable` |
| `SBSelect` | `SelectQuery` |

### SQL Execution / Externalized SQL
| old | new |
|---|---|
| `SBQueryExecutor` | `SqlRunner` |
| `SBSqlFileLoader` | `SqlFileLoader` |
| `SBSqlParser` / `SBSqlProcessor` / `SBSqlFormatter` | `SqlParser` / `SqlProcessor` / `SqlFormatter` |
| `SBMyBatisSqlProcessor` | `MyBatisSqlProcessor` |
| `SBDialect` | `Dialect` |

### Entity / Meta / Mapping
| old | new |
|---|---|
| `SBEntityMapper` / `SBMyBatisMapper` | `EntityMapper` / `MyBatisMapper` |
| `SBEntityOperations` | `EntityOperations` |
| `SBPrimaryKeyInfo` | `PrimaryKeyInfo` |
| `SBOptimisticLockSupport` | `OptimisticLockSupport` |
| `@SBTableMeta` / `@SBColumnMeta` | `@FluidTable` / `@FluidColumn` |

### Config / i18n
| old | new |
|---|---|
| `SBMyBatisConfig` | `FluidConfig` |
| `SBOptimisticLockConfig(+Loader)` | `OptimisticLockConfig(+Loader)` |
| `SBLocaleConfig` / `SBMessageManager` | `FluidLocale` / `Messages` |

### Transaction
| old | new |
|---|---|
| `SBTransactionManager` / `SBTransactionOperation` | `TransactionManager` / `TransactionOperation` |
| `SBTransactionContext` / `SBTransactionCallback` | `TransactionContext` / `TransactionCallback` |
| `SBThreadLocalDataSource` | `ThreadLocalDataSource` |

### Exceptions
| old | new |
|---|---|
| `SBException` / `SBSQLException` | `FluidException` / `FluidSqlException` |
| `SBNoResultException` / `SBNonUniqueResultException` | `NoResultException` / `NonUniqueResultException` |
| `SBOptimisticLockException` | `OptimisticLockException` |
| `SBSqlParseException` | `SqlParseException` |
| `SBTypeConversionException` | `TypeConversionException` |
| `SBEntityException` / `SBIllegalStateException` / `SBTransactionException` | `EntityException` / `FluidIllegalStateException` (or JDK `IllegalStateException`) / `TransactionException` |

### Spring
| old | new |
|---|---|
| `SeasarBatisAutoConfiguration` | `BatisFluidAutoConfiguration` |
| `SpringJdbcManager` | `SpringJdbcFlow` |

---

## Safe Replace Order

1) artifactId -> 2) package -> 3) types.  
Exceptions and annotations first (small blast radius), then the fluent API.  
Verify with integration tests (H2 + MySQL/Postgres/Oracle/SQLServer via Testcontainers).

---

## Examples: Before / After

### Before
```java
SBJdbcManager manager = new SBJdbcManager(sqlSessionFactory);
List<User> list = manager.from(User.class)
    .where().eq("status", "ACTIVE")
    .getResultList();
```

### After
```java
var fluid = BatisFluid.of(sqlSessionFactory);
List<User> list = fluid.jdbcFlow()
    .from(User.class)
    .where(w -> w.eq("status", "ACTIVE"))
    .getResultList();
```

---