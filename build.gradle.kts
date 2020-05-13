import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm").version("1.3.72")
    application
}

repositories {
    jcenter()
}

application {
    mainClassName = "io.ktor.server.netty.EngineMain"
}

dependencies {
    implementation(platform("io.ktor:ktor-bom:1.3.1"))
    implementation(platform("org.apache.logging.log4j:log4j-bom:2.13.2"))

    implementation(kotlin("stdlib-jdk8"))
    implementation("io.ktor:ktor-server-netty")
    implementation("by.dev.madhead.telek:model:0.0.5")
    implementation("com.github.insanusmokrassar:TelegramBotAPI-extensions-api:0.27.2")
    implementation("org.apache.logging.log4j:log4j-core")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl")
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "13"
    }
}
