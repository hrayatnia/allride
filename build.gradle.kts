plugins {
    kotlin("jvm") version "1.9.21"
    kotlin("plugin.serialization") version "1.9.21"
    id("io.ktor.plugin") version "2.3.8"
    application
    id("com.google.protobuf") version "0.9.4"
}

group = "me.rayatnia"
version = "0.0.1"

application {
    mainClass.set("io.ktor.server.netty.EngineMain")
}

repositories {
    mavenCentral()
}

val ktorVersion = "2.3.8"
val kotlinVersion = "1.9.21"
val logbackVersion = "1.4.14"
val awsSdkVersion = "2.24.12"
val opencsvVersion = "5.9"
val mockkVersion = "1.13.9"
val protobufVersion = "3.25.3"
val grpcKotlinVersion = "1.4.1"
val grpcJavaVersion = "1.61.0"
val coroutinesVersion = "1.7.3"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs = listOf("-Xjsr305=strict")
    }
}

dependencies {
    // Ktor Server
    implementation("io.ktor:ktor-server-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-netty-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-status-pages-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-default-headers-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-host-common-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jvm:$ktorVersion")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:$ktorVersion")
    
    // Ktor Client
    implementation("io.ktor:ktor-client-core-jvm:$ktorVersion")
    implementation("io.ktor:ktor-client-cio-jvm:$ktorVersion")
    
    // Ktor Serialization
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktorVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    
    // Protobuf and gRPC
    implementation("com.google.protobuf:protobuf-kotlin:$protobufVersion")
    implementation("io.grpc:grpc-kotlin-stub:$grpcKotlinVersion")
    implementation("io.grpc:grpc-protobuf:$grpcJavaVersion")
    implementation("io.grpc:grpc-netty-shaded:$grpcJavaVersion")
    
    // AWS
    implementation(platform("software.amazon.awssdk:bom:$awsSdkVersion"))
    implementation("software.amazon.awssdk:sqs")
    
    // CSV Processing
    implementation("com.opencsv:opencsv:$opencsvVersion")
    
    // Logging
    implementation("ch.qos.logback:logback-classic:$logbackVersion")
    implementation("org.slf4j:slf4j-api:2.0.11")
    
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    
    // Testing
    testImplementation("io.ktor:ktor-server-tests-jvm:$ktorVersion")
    testImplementation("io.ktor:ktor-server-test-host:$ktorVersion")
    testImplementation("org.jetbrains.kotlin:kotlin-test:$kotlinVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testImplementation("io.grpc:grpc-testing:$grpcJavaVersion")
    testImplementation("io.grpc:grpc-inprocess:$grpcJavaVersion")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

protobuf {
    protoc {
        artifact = "com.google.protobuf:protoc:$protobufVersion"
    }
    plugins {
        this.create("grpckt") {
            artifact = "io.grpc:protoc-gen-grpc-kotlin:$grpcKotlinVersion:jdk8@jar"
        }
        this.create("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:$grpcJavaVersion"
        }
    }
    generateProtoTasks {
        all().forEach { task ->
            task.builtins {
                kotlin {}
                java {}
            }
            task.plugins {
                this.create("grpckt")
                this.create("grpc")
            }
        }
    }
}

sourceSets.main {
    kotlin.srcDir("build/generated/source/proto/main/kotlin")
    kotlin.srcDir("build/generated/source/proto/main/grpckt")
    java.srcDir("build/generated/source/proto/main/java")
    java.srcDir("build/generated/source/proto/main/grpc")
}
