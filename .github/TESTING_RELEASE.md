# Release Workflow Testing Guide

This document explains how to test the Maven Central release workflow without performing actual releases.

## Testing Methods

### 1. Pull Request Testing (Automatic)

The release workflow automatically runs in test mode when:
- A pull request modifies workflow or build files:
  - `.github/workflows/release.yml`
  - `build.gradle`
  - `lib/build.gradle`
  - `spring/build.gradle`
  - `settings.gradle`

**What happens in PR test mode:**
- ✅ Builds both modules
- ✅ Validates publishing configuration
- ✅ Uses test version `1.0.0-test`
- ⚠️ Skips GPG setup and Maven Central publishing

### 2. Manual Dry Run Testing

You can manually test the workflow using GitHub Actions:

1. Go to **Actions** → **Release to Maven Central**
2. Click **"Run workflow"**
3. Fill in the parameters:
   - **Version**: `1.0.0` (or desired version)
   - **Dry run mode**: ✅ **Check this box**
4. Click **"Run workflow"**

**What happens in dry run mode:**
- ✅ Builds both modules
- ✅ Validates publishing configuration  
- ✅ Uses version `{your-version}-test`
- ⚠️ Skips GPG setup and Maven Central publishing

### 3. Local Testing

You can test the workflow logic locally:

```bash
# Set test environment
export DRY_RUN=true
export RELEASE_VERSION=1.0.0-test
export MODE=TEST

# Update versions (will be reverted)
sed -i "s/version = '[^']*'/version = '$RELEASE_VERSION'/" lib/build.gradle
sed -i "s/version = '[^']*'/version = '$RELEASE_VERSION'/" spring/build.gradle

# Test build and publishing tasks
./gradlew clean build -x test
./gradlew tasks --all | grep -i publish

# Revert changes
git checkout -- lib/build.gradle spring/build.gradle
```

## Production Release

### Tag-based Release
```bash
git tag v1.0.0
git push origin v1.0.0
```

### Manual Release
1. Go to **Actions** → **Release to Maven Central**
2. Click **"Run workflow"**
3. Fill in the parameters:
   - **Version**: `1.0.0`
   - **Dry run mode**: ⬜ **Leave unchecked**
4. Click **"Run workflow"**

## Workflow Validation

The workflow validates:
- ✅ Build succeeds for both modules
- ✅ Publishing tasks are available
- ✅ JAR artifacts are generated
- ✅ Version updates work correctly
- ✅ GPG setup (production only)
- ✅ Maven Central publishing (production only)

## Required Secrets (Production Only)

For production releases, configure these GitHub secrets:
- `OSSRH_USERNAME` / `OSSRH_PASSWORD`
- `GPG_PRIVATE_KEY` / `GPG_PASSPHRASE`
- `SIGNING_KEY_ID` / `SIGNING_PASSWORD` / `SIGNING_SECRET_KEY`

## Generated Artifacts

The workflow generates these artifacts for each module:
- Main JAR: `{module}-{version}.jar`
- Sources JAR: `{module}-{version}-sources.jar`
- Javadoc JAR: `{module}-{version}-javadoc.jar`

Example output:
```
./lib/build/libs/lib-1.0.0.jar
./spring/build/libs/spring-1.0.0.jar
```