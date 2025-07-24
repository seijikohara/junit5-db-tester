plugins {
    id("java")
    alias(libs.plugins.spotless)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    // Main library dependency
    testImplementation(project(":lib"))
    
    // JUnit 5
    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:${libs.versions.junit.get()}")
    
    // Testcontainers
    testImplementation("org.testcontainers:junit-jupiter:1.20.5")
    testImplementation("org.testcontainers:mysql:1.20.5")
    
    // MySQL Driver
    testImplementation("com.mysql:mysql-connector-j:8.4.0")
    
    // Spring JDBC for DataSource utilities
    testImplementation("org.springframework:spring-jdbc:6.1.8")
    
    // SLF4J for logging
    testImplementation("org.slf4j:slf4j-simple:2.0.16")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    
    // Testcontainers requires Docker
    systemProperty("junit.jupiter.execution.parallel.enabled", "false")
}

spotless {
    java {
        googleJavaFormat()
        removeUnusedImports()
        trimTrailingWhitespace()
        endWithNewline()
    }
}