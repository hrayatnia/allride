plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor)
    id("org.jetbrains.dokka") version "1.9.10"
    application
    kotlin("plugin.serialization") version "1.9.22"
}

group = "me.rayatnia"
version = "0.0.1"

application {
    mainClass.set("io.ktor.server.netty.EngineMain")
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
    maven {
        url = uri("https://packages.confluent.io/maven/")
    }
}

val exposedVersion = "0.46.0"
val postgresVersion = "42.7.1"
val hikariVersion = "5.1.0"
val awsSdkVersion = "2.24.12"
val xmemcachedVersion = "2.4.8"
val ktorVersion = "2.3.7"
val junitVersion = "5.12.1"

dependencies {
    // Ktor Core
    implementation(libs.ktor.server.call.logging)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.swagger)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.config.yaml)
    implementation(libs.ktor.server.kafka)
    
    // Serialization
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation:$ktorVersion")
    
    // AWS SDK
    implementation(platform("software.amazon.awssdk:bom:$awsSdkVersion"))
    implementation("software.amazon.awssdk:sqs")
    
    // Database
    implementation("org.jetbrains.exposed:exposed-core:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-dao:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-jdbc:$exposedVersion")
    implementation("org.jetbrains.exposed:exposed-java-time:$exposedVersion")
    implementation("org.postgresql:postgresql:$postgresVersion")
    implementation("com.zaxxer:HikariCP:$hikariVersion")
    
    // Memcached
    implementation("com.googlecode.xmemcached:xmemcached:$xmemcachedVersion")
    
    // Logging
    implementation(libs.logback.classic)
    
    // Testing
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5:2.1.10")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
    
    // Add these explicit dependencies to ensure version alignment
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.12.1")
    testRuntimeOnly("org.junit.platform:junit-platform-engine:1.12.1")
    testRuntimeOnly("org.junit.platform:junit-platform-commons:1.12.1")
}

tasks.test {
    useJUnitPlatform()
}

// Configure distribution
tasks.installDist {
    destinationDir = file("$buildDir/install/allride")
}
