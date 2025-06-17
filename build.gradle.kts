plugins {
    id("org.jetbrains.kotlin.jvm") version "1.9.25"
    id("org.jetbrains.kotlin.plugin.allopen") version "1.9.25"
    id("com.google.devtools.ksp") version "1.9.25-1.0.20"
    id("io.micronaut.application") version "4.5.3"
    id("com.gradleup.shadow") version "8.3.6"
    id("io.micronaut.aot") version "4.5.3"
}

version = "0.1"
group = "br.com.estapar"

val kotlinVersion=project.properties.get("kotlinVersion")
repositories {
    mavenCentral()
}

dependencies {
    runtimeOnly("org.yaml:snakeyaml")
    ksp("io.micronaut.data:micronaut-data-processor")
    ksp("io.micronaut:micronaut-http-validation")
    ksp("io.micronaut.jsonschema:micronaut-json-schema-processor")
    ksp("io.micronaut.serde:micronaut-serde-processor")
    ksp("io.micronaut.spring:micronaut-spring-annotation")
    ksp("io.micronaut.validation:micronaut-validation-processor")
    implementation("io.micronaut:micronaut-http-client")
    implementation("io.micronaut:micronaut-management")
    implementation("io.micronaut.data:micronaut-data-jdbc")
    implementation("io.micronaut.data:micronaut-data-spring")
    implementation("io.micronaut.flyway:micronaut-flyway")
    implementation("io.micronaut.jsonschema:micronaut-json-schema-annotations")
    implementation("io.micronaut.kotlin:micronaut-kotlin-extension-functions")
    implementation("io.micronaut.kotlin:micronaut-kotlin-runtime")
    implementation("io.micronaut.serde:micronaut-serde-jackson")
    implementation("io.micronaut.sql:micronaut-jdbc-hikari")
    implementation("io.micronaut.validation:micronaut-validation")
    implementation("jakarta.annotation:jakarta.annotation-api")
    implementation("jakarta.transaction:jakarta.transaction-api")
    implementation("jakarta.validation:jakarta.validation-api")
    implementation("org.jetbrains.kotlin:kotlin-reflect:${kotlinVersion}")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:${kotlinVersion}")
    implementation("org.springframework:spring-jdbc")
    implementation("org.springframework.boot:spring-boot-starter")
    runtimeOnly("ch.qos.logback:logback-classic")
    runtimeOnly("org.slf4j:slf4j-api")
    runtimeOnly("com.fasterxml.jackson.module:jackson-module-kotlin")
    runtimeOnly("com.h2database:h2")
    testImplementation("io.micronaut.jsonschema:micronaut-json-schema-validation")
    implementation("org.postgresql:postgresql")
    runtimeOnly("org.flywaydb:flyway-database-postgresql")
    implementation("org.flywaydb:flyway-commandline:10.0.0")
    implementation("io.micronaut.micrometer:micronaut-micrometer-core")
}

allOpen {
    annotation("jakarta.inject.Singleton")
    annotation("io.micronaut.context.annotation.Bean")
    annotation("io.micronaut.context.annotation.Factory")
    annotation("io.micronaut.context.annotation.Configuration")
    annotation("io.micronaut.context.annotation.Executable")
    annotation("jakarta.transaction.Transactional")
    annotation("io.micronaut.aop.Around")
}


application {
    mainClass = "br.com.estapar.ApplicationKt"
}
java {
    sourceCompatibility = JavaVersion.toVersion("17")
}


graalvmNative.toolchainDetection = false

micronaut {
    runtime("netty")
    testRuntime("junit5")
    processing {
        incremental(true)
        annotations("br.com.estapar.*")
    }
    aot {
        // Please review carefully the optimizations enabled below
        // Check https://micronaut-projects.github.io/micronaut-aot/latest/guide/ for more details
        optimizeServiceLoading = false
        convertYamlToJava = false
        precomputeOperations = true
        cacheEnvironment = true
        optimizeClassLoading = true
        deduceEnvironment = true
        optimizeNetty = true
        replaceLogbackXml = true
    }
}

tasks.withType<Jar> {
    isZip64 = true
}


