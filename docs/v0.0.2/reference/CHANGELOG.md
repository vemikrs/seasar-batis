# CHANGELOG

## [0.0.2] - 2025-10-06
### Added
- Rebrand to **BatisFluid** (`jp.vemi:batis-fluid-core`, `jp.vemi:batis-fluid-spring`)
- New entrypoint **BatisFluid**, fluent API **JdbcFlow**, and SQL facade **SqlRunner**
- New annotations **@FluidTable**, **@FluidColumn**
- New config **OptimisticLockConfig(+Loader)**, i18n **FluidLocale / Messages**

### Changed
- Package moved: `jp.vemi.seasarbatis` -> `jp.vemi.batisfluid`
- Massive class renames (see `NAMING_REFACTOR_PLAN.md`)

### Deprecated
- All legacy `SB*` classes kept as thin adapters (planned removal in >= `0.0.3`)
- Legacy properties filename still accepted (`seasarbatis-optimistic-lock.properties`)

### Fixed
- Minor i18n keys and SQL parser edge-cases from 0.0.1

---

## [0.0.1] - 2025-10-06
- Initial pre-release as **SeasarBatis**: MyBatis wrapper with S2-like `JdbcManager` ops,
  SQL parser/processor, optimistic locking, and Spring integration (baseline).