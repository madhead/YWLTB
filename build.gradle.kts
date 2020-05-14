import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI
import java.util.Properties

plugins {
    kotlin("jvm").version("1.3.72")
    application
    id("org.liquibase.gradle").version("2.0.2")
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
    implementation("org.postgresql:postgresql:42.2.12")
    implementation("org.apache.logging.log4j:log4j-core")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl")

    liquibaseRuntime("org.liquibase:liquibase-core:3.8.9")
    liquibaseRuntime("org.yaml:snakeyaml:1.26")
    liquibaseRuntime("org.postgresql:postgresql:42.2.12")
}

liquibase {
    activities {
        register("ywltb") {
            val env = Properties().apply {
                load(project.file(".env").bufferedReader())
            }
            val databaseUri = URI(env["DATABASE_URL"].toString())

            this.arguments = mapOf(
                    "url" to "jdbc:postgresql://" + databaseUri.host + ':' + databaseUri.port + databaseUri.path,
                    "username" to databaseUri.userInfo.split(":")[0],
                    "password" to databaseUri.userInfo.split(":")[1],
                    "driver" to "org.postgresql.Driver",
                    "changeLogFile" to "src/main/liquibase/changelog.yml"
            )
        }
    }
}

tasks {
    withType<KotlinCompile> {
        kotlinOptions.jvmTarget = "13"
    }
}
