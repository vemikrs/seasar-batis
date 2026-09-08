plugins {
    // Apply the java-library plugin for API and implementation separation.
    id("java-library")
    id("com.vanniktech.maven.publish")
    id("jacoco")
    id("com.github.ben-manes.versions") version "0.61.0"
}

import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar

group = "jp.vemi"
version = "0.0.3"

repositories { mavenCentral() }

dependencies {
    // Use JUnit Jupiter for testing.
    testImplementation("org.junit.jupiter:junit-jupiter-api:6.1.3")
    testImplementation("org.mockito:mockito-junit-jupiter:5.23.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:6.1.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:6.1.3")

    // Mockito for mocking in tests.
    testImplementation("org.mockito:mockito-core:5.23.0")

    // Database dependencies for testing.
    testImplementation("com.h2database:h2:2.5.250")
    testImplementation("com.mysql:mysql-connector-j:9.7.0")
    testImplementation("org.postgresql:postgresql:42.7.13")
    testImplementation("com.microsoft.sqlserver:mssql-jdbc:13.4.0.jre11")
    testImplementation("com.oracle.database.jdbc:ojdbc11:23.26.3.0.0")

    // Testcontainers for integration testing (align to latest stable)
    testImplementation("org.testcontainers:testcontainers-junit-jupiter:2.0.5")
    testImplementation("org.testcontainers:testcontainers-mysql:2.0.5")
    testImplementation("org.testcontainers:testcontainers-postgresql:2.0.5")
    testImplementation("org.testcontainers:testcontainers-mssqlserver:2.0.5")
    testImplementation("org.testcontainers:testcontainers-oracle-xe:2.0.5")

    // AssertJ for fluent assertions
    testImplementation("org.assertj:assertj-core:3.27.7")

    // This dependency is exported to consumers, that is to say found on their compile classpath.
    api("org.mybatis:mybatis:3.5.19")
    api("org.mybatis:mybatis-spring:4.1.0")

    // This dependency is used internally, and not exposed to consumers on their own compile classpath.
    implementation("org.mybatis.generator:mybatis-generator-core:2.0.0")
    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation("org.apache.commons:commons-lang3:3.20.0")
    implementation("com.google.guava:guava:33.7.1-jre")
    implementation("org.apache.commons:commons-dbcp2:2.14.0")

    // Runtime only dependencies are not added to the compile classpath of projects that depend on this project.
    runtimeOnly("com.mysql:mysql-connector-j:9.7.0")

    // Lombok for generating boilerplate code.
    compileOnly("org.projectlombok:lombok:1.18.48")
    annotationProcessor("org.projectlombok:lombok:1.18.48")
    testCompileOnly("org.projectlombok:lombok:1.18.48")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.48")

    // SLF4J for logging
    implementation("org.slf4j:slf4j-api:2.0.19")
    implementation("ch.qos.logback:logback-classic:1.6.3")
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

tasks.named<org.gradle.jvm.tasks.Jar>("jar").configure {
    // Maven Central 上のアーティファクト名と揃えるため、JAR のベース名を明示します。
    archiveBaseName.set("batis-fluid-core")
}

tasks.named<Test>("test").configure {
    useJUnitPlatform {
        val prop = System.getProperty("junitTags") ?: project.findProperty("junitTags")?.toString()
        if (!prop.isNullOrBlank()) {
            val tags = prop.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            if (tags.isNotEmpty()) includeTags(*tags.toTypedArray())
        } else {
            // デフォルトではintegrationとv0.0.1テストを除外
            excludeTags("integration", "v0.0.1")
        }
    }
    maxParallelForks = 1
    reports { html.required.set(true); junitXml.required.set(true) }
}

// 統合テスト専用タスク
tasks.register<Test>("integrationTest") {
    description = "Runs integration tests tagged with 'integration'"
    group = "verification"
    useJUnitPlatform {
        includeTags("integration")
    }
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    maxParallelForks = 1
    reports { html.required.set(true); junitXml.required.set(true) }
    shouldRunAfter(tasks.named("test"))
}

tasks.withType<Javadoc>().configureEach {
    val opts = options as? CoreJavadocOptions
    opts?.addStringOption("Xdoclint:none", "-quiet")
    opts?.addStringOption("Xmaxwarns", "1")
    // JDK 21 の javadoc は --allow-script-in-comments を要求します（Gradle 側が先頭に '-' を付与するため、ここでは先頭に '-' を付けます）
    opts?.addBooleanOption("-allow-script-in-comments", true)
    isFailOnError = false
}

tasks.named<JacocoReport>("jacocoTestReport").configure {
    reports { xml.required.set(false); csv.required.set(false); html.required.set(true) }
}

mavenPublishing {
    // 「何を公開するか」はプラグインの公式 API で定義します（Javadoc/Sources の重複を防止）。
    configure(JavaLibrary(javadocJar = JavadocJar.Javadoc(), sourcesJar = SourcesJar.Sources()))
    publishToMavenCentral()

    val signingKey = (findProperty("signingInMemoryKey") as? String)
        ?: System.getenv("ORG_GRADLE_PROJECT_signingInMemoryKey")
    if (!signingKey.isNullOrBlank()) {
        signAllPublications()
    }

    coordinates("jp.vemi", "batis-fluid-core", version.toString())
    pom {
        name.set("BatisFluid Core")
        description.set("Modern, minimal, pluggable MyBatis wrapper with fluent API and externalized SQL support")
        url.set("https://github.com/vemikrs/batis-fluid")
        licenses {
            license {
                name.set("The Apache License, Version 2.0")
                url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
            }
        }
        developers {
            developer {
                id.set("vemikrs")
                name.set("Hiroki Kurosawa")
                email.set("contact@vemi.jp")
            }
        }
        scm {
            connection.set("scm:git:git://github.com/vemikrs/batis-fluid.git")
            developerConnection.set("scm:git:ssh://git@github.com/vemikrs/batis-fluid.git")
            url.set("https://github.com/vemikrs/batis-fluid")
        }
    }
}
