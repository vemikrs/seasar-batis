plugins {
    // Apply the java-library plugin for API and implementation separation.
    id("java-library")
    id("com.vanniktech.maven.publish")
    id("jacoco")
}

group = "jp.vemi"
version = "1.0.0-beta.2"

repositories { mavenCentral() }

dependencies {
    // Use JUnit Jupiter for testing.
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.11.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.3.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.11.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.11.0")

    // Mockito for mocking in tests.
    testImplementation("org.mockito:mockito-core:5.2.0")

    // Database dependencies for testing.
    testImplementation("com.h2database:h2:2.3.232")
    testImplementation("mysql:mysql-connector-java:8.0.33")

    // TestContainers for integration testing
    testImplementation("org.testcontainers:junit-jupiter:1.19.3")
    testImplementation("org.testcontainers:mysql:1.19.3")

    // AssertJ for fluent assertions
    testImplementation("org.assertj:assertj-core:3.24.2")

    // This dependency is exported to consumers, that is to say found on their compile classpath.
    api("org.mybatis:mybatis:3.5.13")
    api("org.mybatis:mybatis-spring:3.0.2")

    // This dependency is used internally, and not exposed to consumers on their own compile classpath.
    implementation("org.mybatis.generator:mybatis-generator-core:1.4.2")
    implementation("org.apache.commons:commons-math3:3.6.1")
    implementation("org.apache.commons:commons-lang3:3.12.0")
    implementation("com.google.guava:guava:31.1-jre")
    implementation("org.apache.commons:commons-dbcp2:2.13.0")

    // Runtime only dependencies are not added to the compile classpath of projects that depend on this project.
    runtimeOnly("mysql:mysql-connector-java:8.0.33")

    // Lombok for generating boilerplate code.
    compileOnly("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
    testCompileOnly("org.projectlombok:lombok:1.18.30")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.30")

    // SLF4J for logging
    implementation("org.slf4j:slf4j-api:2.0.1")
    implementation("ch.qos.logback:logback-classic:1.4.5")
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(21)) }
    withSourcesJar()
    withJavadocJar()
}

tasks.named<Test>("test").configure {
    useJUnitPlatform()
    val prop = System.getProperty("junitTags") ?: findProperty("junitTags")?.toString()
    if (!prop.isNullOrBlank()) {
        val tags = prop.split(',').map { it.trim() }.filter { it.isNotEmpty() }
        if (tags.isNotEmpty()) includeTags(*tags.toTypedArray())
    }
    maxParallelForks = 1
    reports { html.required.set(true); junitXml.required.set(true) }
}

tasks.withType<Javadoc>().configureEach {
    val opts = options as? CoreJavadocOptions
    opts?.addStringOption("Xdoclint:none", "-quiet")
    opts?.addStringOption("Xmaxwarns", "1")
    isFailOnError = false
}

tasks.named<JacocoReport>("jacocoTestReport").configure {
    reports { xml.required.set(false); csv.required.set(false); html.required.set(true) }
}

import com.vanniktech.maven.publish.SonatypeHost
mavenPublishing {
    publishToMavenCentral(SonatypeHost.CENTRAL_PORTAL)
    signAllPublications()
    coordinates("jp.vemi", "seasar-batis", version.toString())
    pom {
        name.set("SeasarBatis")
        description.set("Seasar2-like MyBatis wrapper library that provides JdbcManager-like operations")
        url.set("https://github.com/vemikrs/seasar-batis")
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
            connection.set("scm:git:git://github.com/vemikrs/seasar-batis.git")
            developerConnection.set("scm:git:ssh://git@github.com/vemikrs/seasar-batis.git")
            url.set("https://github.com/vemikrs/seasar-batis")
        }
    }
}
