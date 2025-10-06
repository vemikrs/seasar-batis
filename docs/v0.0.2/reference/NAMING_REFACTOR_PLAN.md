# NAMING REFACTOR PLAN (v0.0.2)

Principles:
- Remove `SB` prefix everywhere.
- Keep "Flow" (fluent API) and "Sql/SqlFile" (externalized SQL) as first-class concepts.
- Prefer clear domain names over framework prefixes.

## Artifacts & Package

- `jp.vemi:seasar-batis` -> `jp.vemi:batis-fluid-core`
- `jp.vemi:seasar-batis-spring` -> `jp.vemi:batis-fluid-spring`
- package: `jp.vemi.seasarbatis` -> `jp.vemi.batisfluid`

## Full Rename Table

### 1) Facade / Management
SBJdbcManager -> JdbcFlow
SBJdbcManagerFactory -> BatisFluid
SBSqlSessionFactory -> SqlSessionGateway (optional)

### 2) Query / Builder
SBSelectBuilder -> SelectFlow
SBUpdateBuilder -> UpdateFlow
SBDeleteBuilder -> DeleteFlow
SBSelect -> SelectQuery
SBWhere -> Where
SimpleWhere -> SimpleWhere
ComplexWhere -> ComplexWhere
AbstractWhere -> AbstractWhere
SBOrderByCapable -> OrderByCapable
SBWhereCapable -> WhereCapable

### 3) SQL Execution / Externalized SQL
SBQueryExecutor -> SqlRunner
SBSqlFileLoader -> SqlFileLoader
SBSqlParser -> SqlParser
SBSqlProcessor -> SqlProcessor
SBSqlFormatter -> SqlFormatter
SBMyBatisSqlProcessor -> MyBatisSqlProcessor
SBDialect -> Dialect
CommandType -> CommandType
ParsedSql -> ParsedSql
ProcessedSql -> ProcessedSql

### 4) Entity / Meta / Mapping
SBEntityMapper -> EntityMapper
SBMyBatisMapper -> MyBatisMapper
SBEntityOperations -> EntityOperations
SBPrimaryKeyInfo -> PrimaryKeyInfo
SBOptimisticLockSupport -> OptimisticLockSupport
@SBTableMeta -> @FluidTable
@SBColumnMeta -> @FluidColumn

### 5) Config / i18n
SBMyBatisConfig -> FluidConfig
SBOptimisticLockConfig -> OptimisticLockConfig
SBOptimisticLockConfigLoader -> OptimisticLockConfigLoader
SBLocaleConfig -> FluidLocale
SBMessageManager -> Messages

### 6) Transaction
SBTransactionManager -> TransactionManager
SBTransactionOperation -> TransactionOperation
SBTransactionContext -> TransactionContext
SBTransactionCallback -> TransactionCallback
SBThreadLocalDataSource -> ThreadLocalDataSource

### 7) Exceptions
SBException -> FluidException
SBSQLException -> FluidSqlException
SBNoResultException -> NoResultException
SBNonUniqueResultException -> NonUniqueResultException
SBOptimisticLockException -> OptimisticLockException
SBSqlParseException -> SqlParseException
SBTypeConversionException -> TypeConversionException
SBEntityException -> EntityException
SBIllegalStateException -> FluidIllegalStateException (or JDK IllegalStateException)
SBTransactionException -> TransactionException

### 8) Spring Integration
SeasarBatisAutoConfiguration -> BatisFluidAutoConfiguration
SpringJdbcManager -> SpringJdbcFlow

## Adapters (Deprecated)
- Keep old `SB*` classes as thin adapters with `@Deprecated(since="0.0.2")`.
- Plan removal in >= v0.0.3. Mention EOL in CHANGELOG.

## Regex Hints

- Class prefix: `\bSB([A-Z][A-Za-z0-9_]*)` -> `$1`
- Package: `jp\.vemi\.seasarbatis` -> `jp.vemi.batisfluid`
- Config file: `seasarbatis-optimistic-lock\.properties` -> `batisfluid-optimistic-lock.properties`