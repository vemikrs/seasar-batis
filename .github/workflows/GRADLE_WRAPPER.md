# Gradle Wrapper Configuration

This project follows the standard practice of **NOT** committing `gradle-wrapper.jar` to the Git repository. 

## Why gradle-wrapper.jar is excluded

1. **Repository size**: Binary files unnecessarily increase repository size
2. **Version control**: JAR files don't diff well in Git
3. **CI/CD best practices**: Modern CI systems handle wrapper downloads automatically
4. **Security**: Wrapper validation ensures integrity without committed binaries

## How it works

1. **Local development**: The `gradlew` script automatically downloads the wrapper JAR on first use
2. **CI/CD environment**: The `gradle/gradle-build-action` GitHub Action handles wrapper setup automatically
3. **Validation**: `gradle/wrapper-validation-action` ensures wrapper integrity and security

## Manual setup (if needed)

If you need to manually download the wrapper JAR:

```bash
# The gradlew script will download it automatically, but if needed:
curl -L -o gradle/wrapper/gradle-wrapper.jar \
  "https://github.com/gradle/gradle/raw/v8.9.0/gradle/wrapper/gradle-wrapper.jar"
```

## Gradle version

This project uses Gradle 8.9 as specified in `gradle/wrapper/gradle-wrapper.properties`.