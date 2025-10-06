# Repository Guidelines

## Project Structure & Module Organization
- `lib/` hosts the core SeasarBatis library (`src/main/java` for Java sources, `src/main/resources` for MyBatis configs such as `mybatis-config.xml`, and `src/test` for fixtures under `resources/sql`).
- `spring/` wraps the core module with Spring support; `src/main/java/jp/vemi/seasarbatis/spring` contains the auto-configuration layer, while `src/test/java` mirrors package paths for Spring-specific tests.
- Gradle wrapper files (`gradlew`, `gradlew.bat`, `gradle/`) must be kept in sync when changing Gradle versions to ensure reproducible builds.

## Build, Test, and Development Commands
- `./gradlew build` compiles every module and runs the default test suite.
- `./gradlew :lib:test` executes library tests; append `-DjunitTags=integration` to opt in to container-backed integration cases otherwise excluded.
- `./gradlew :spring:test` targets Spring module tests only and is useful when iterating on auto-configuration changes.
- `./gradlew :lib:jacocoTestReport` produces an HTML coverage report in `lib/build/reports/jacoco/test/html`.

## Coding Style & Naming Conventions
- Java 21 is enforced via the Gradle toolchain; use 4-space indentation and place braces on the same line as declarations, matching existing sources.
- Prefer `UpperCamelCase` for classes/interfaces, `lowerCamelCase` for methods and fields, and `SCREAMING_SNAKE_CASE` for constants.
- Document public APIs with Javadoc (see `SBSqlSessionFactory` for tone), and keep log messages in English even when comments contain Japanese context.
- Lombok is available (`@Getter`, `@Builder`, etc.) but keep generated code obvious—include concise Javadoc and avoid hiding complex logic in Lombok annotations.

## Testing Guidelines
- Unit tests rely on JUnit Jupiter, Mockito, and AssertJ; place them under matching package directories in `src/test/java` and suffix classes with `Test`.
- Integration tests use Testcontainers and are tagged `integration`; only enable them locally or in CI environments that can run Docker.
- Keep reusable SQL fixtures in `src/test/resources/sql` and prefer the provided in-memory configurations over ad-hoc files.
- After significant changes, regenerate coverage via `jacocoTestReport` and review the HTML output before submitting.

## Commit & Pull Request Guidelines
- Follow the conventional `type: summary` convention seen in history (`feat`, `test`, `fix`, `docs`, `ci`); write summaries in the imperative mood and limit them to ~72 characters.
- Reference related issues in the body, describe behavioral or schema changes, and document test commands you ran.
- For PRs, include screenshots or logs when altering SQL generation, note any required configuration changes (e.g., updates to `mybatis-config.xml`), and ensure both unit and optional integration suites are accounted for.
- CLIでの確認結果や補足コメントは日本語で記載し、レビュー時のやり取りも日本語で統一してください。
