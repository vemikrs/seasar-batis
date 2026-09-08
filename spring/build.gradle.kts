plugins {
    id("java-library")
    id("com.vanniktech.maven.publish")
}

import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar

group = "jp.vemi"
version = "0.0.3"

sourceSets {
    named("main") {
        java.srcDirs("src/main/java")
        resources.srcDirs("src/main/resources")
    }
    named("test") {
        java.srcDirs("src/test/java")
        resources.srcDirs("src/test/resources")
    }
}

repositories { mavenCentral() }

dependencies {
    api(project(":lib"))
    api("org.springframework.boot:spring-boot-autoconfigure:4.1.1")
    api("org.springframework:spring-context:7.0.9")
    api("org.springframework:spring-beans:7.0.9")
    api("org.mybatis:mybatis-spring:4.1.0")

    // Align Spring 6.x
    implementation("org.springframework:spring-tx:7.0.9")
    implementation("org.springframework:spring-jdbc:7.0.9")

    testImplementation("org.springframework.boot:spring-boot-starter-test:4.1.1")
    // Override transitive assertj-core to fix XXE vulnerability (CVE-2026-24400)
    testImplementation("org.assertj:assertj-core:3.27.7")
    testImplementation("org.junit.jupiter:junit-jupiter-api:6.1.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:6.1.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("com.h2database:h2:2.5.250")
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
}

tasks.named<org.gradle.jvm.tasks.Jar>("jar").configure {
    // Maven Central 上のアーティファクト名と揃えるため、JAR のベース名を明示します。
    archiveBaseName.set("batis-fluid-spring")
}

tasks.named<Test>("test").configure {
    useJUnitPlatform {
        val prop = System.getProperty("junitTags") ?: project.findProperty("junitTags")?.toString()
        if (!prop.isNullOrBlank()) {
            val tags = prop.split(',').map { it.trim() }.filter { it.isNotEmpty() }
            if (tags.isNotEmpty()) includeTags(*tags.toTypedArray())
        }
    }
    // Configure to not fail when no tests are discovered (Gradle 9+)
    failOnNoDiscoveredTests = false
    filter { isFailOnNoMatchingTests = false }
}

tasks.withType<Javadoc>().configureEach {
    val opts = options as? CoreJavadocOptions
    opts?.addStringOption("Xdoclint:none", "-quiet")
    opts?.addStringOption("Xmaxwarns", "1")
    // JDK 21 の javadoc は --allow-script-in-comments を要求します（Gradle 側が先頭に '-' を付与するため、ここでは先頭に '-' を付けます）
    opts?.addBooleanOption("-allow-script-in-comments", true)
    isFailOnError = false
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

    coordinates("jp.vemi", "batis-fluid-spring", version.toString())
    pom {
        name.set("BatisFluid Spring Integration")
        description.set("Spring Framework integration module for BatisFluid")
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

// Duplicate resources handling (Gradle 9 requires explicit strategy on duplicates)
tasks.named<org.gradle.language.jvm.tasks.ProcessResources>("processResources").configure {
    duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.EXCLUDE
}

// Configure sourcesJar task to handle duplicates
tasks.named<org.gradle.jvm.tasks.Jar>("sourcesJar").configure {
    duplicatesStrategy = org.gradle.api.file.DuplicatesStrategy.EXCLUDE
}
