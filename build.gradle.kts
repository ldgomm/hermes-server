plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(ktorLibs.plugins.ktor)
    alias(libs.plugins.kotlin.serialization)
}

group = "com.hermes.backend"
version = "1.0.0-SNAPSHOT"

application {
    mainClass = "io.ktor.server.netty.EngineMain"
}

kotlin {
    jvmToolchain(21)
}
dependencies {
    implementation(ktorLibs.serialization.kotlinx.json)
    implementation(ktorLibs.server.auth)
    implementation(ktorLibs.server.auth.jwt)
    implementation(ktorLibs.server.callLogging)
    implementation(ktorLibs.server.config.yaml)
    implementation(ktorLibs.server.contentNegotiation)
    implementation(ktorLibs.server.core)
    implementation(ktorLibs.server.cors)
    implementation(ktorLibs.server.netty)
    implementation(ktorLibs.server.sessions)

    implementation(ktorLibs.server.statusPages)
    implementation(ktorLibs.server.websockets)
    implementation(libs.koin.ktor)
    implementation(libs.koin.loggerSlf4j)
    implementation(libs.logback.classic)

//    implementation(platform(libs.mongodb.bom))
//    implementation(libs.mongodb.driverSync)
//    implementation(libs.mongodb.driverKotlinCoroutine)

    testImplementation(kotlin("test"))
    testImplementation(ktorLibs.server.testHost)

    implementation("io.github.oshai:kotlin-logging-jvm:7.0.3")
    implementation("io.lettuce:lettuce-core:7.0.0.RELEASE")
    implementation("io.minio:minio:9.0.0")

    implementation("org.mongodb:mongodb-driver-sync:5.7.0")
    implementation("org.mongodb:mongodb-driver-core:5.7.0")
    implementation("org.mongodb:bson:5.7.0")
    implementation("org.mongodb:mongodb-driver-kotlin-coroutine:5.7.0")

    testImplementation("org.testcontainers:mongodb:1.20.4")
    testImplementation("org.testcontainers:junit-jupiter:1.20.4")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.3")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.3")

}

tasks.test {
    useJUnitPlatform()
}

tasks.register<JavaExec>("runMigrationCommand") {
    group = "hermes"
    description = "Runs Hermes MongoDB migrations"

    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.hermes.infrastructure.mongo.migration.MigrationCommandKt")
}