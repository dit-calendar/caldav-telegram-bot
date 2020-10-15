import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

group = "dit-calendar"
version = "0.1.0-SNAPSHOT"

plugins {
    val kotlinVersion = "1.4.10"

    application

    kotlin("jvm") version kotlinVersion

    id("com.github.johnrengelman.shadow") version "5.2.0"
}

application {
    mainClassName = "com.ditcalendar.bot.BotKt"
}

repositories {
    mavenLocal()
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
    jcenter()
}

dependencies {
    val kittinunfResultVersion = "3.1.0"
    val konfigVersion = "1.6.10.0"
    val ktBotVersion = "1.3.5"
    val exposedVersion = "0.28.1"
    val postgresqlVersion = "42.2.16"
    val calDavVersion = "1.0.1"

    implementation("com.github.kittinunf.result:result:$kittinunfResultVersion")

    implementation("com.github.caldav4j:caldav4j:$calDavVersion")
    implementation("javax.cache:cache-api:1.0.0")
    implementation("org.ehcache:ehcache:3.9.0")

    implementation("com.github.elbekd:kt-telegram-bot:$ktBotVersion")
    implementation("com.natpryce:konfig:$konfigVersion")

    implementation("org.jetbrains.exposed", "exposed-core", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-dao", exposedVersion)
    implementation("org.jetbrains.exposed", "exposed-jdbc", exposedVersion)
    implementation("org.postgresql:postgresql:$postgresqlVersion")
}

tasks.withType<KotlinCompile>().configureEach {
    sourceCompatibility = "11"
    kotlinOptions.jvmTarget = "11"

    kotlinOptions.freeCompilerArgs = listOf("-Xjsr305=strict")
    incremental = true
}

tasks.register("stage") {
    dependsOn("build", "clean")
    mustRunAfter("clean")

    //clean up build
    doLast {
        File("build1").mkdirs()
        File("build/libs").copyRecursively(File("build1/libs"))
        delete("build")
        File("build1").renameTo(File("build"))
    }
}