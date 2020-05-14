package by.dev.madhead.ywltb

import by.dev.madhead.ywltb.dao.CredentialsDataStore
import by.dev.madhead.ywltb.dao.UsersDAO
import by.dev.madhead.ywltb.routes.oauth
import by.dev.madhead.ywltb.routes.pages
import by.dev.madhead.ywltb.routes.webhook
import com.github.insanusmokrassar.TelegramBotAPI.extensions.api.telegramBot
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.youtube.YouTubeScopes
import io.ktor.application.Application
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.Compression
import io.ktor.features.DefaultHeaders
import io.ktor.routing.routing
import io.ktor.util.KtorExperimentalAPI
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.UnstableDefault
import org.postgresql.ds.PGSimpleDataSource
import java.net.URI

@ImplicitReflectionSerializer
@UnstableDefault
@KtorExperimentalAPI
fun Application.main() {
    install(DefaultHeaders)
    install(CallLogging)
    install(Compression)

    val databaseUri = URI(environment.config.property("database.url").getString())
    val dataSource = PGSimpleDataSource().apply {
        setUrl("jdbc:postgresql://" + databaseUri.host + ':' + databaseUri.port + databaseUri.path)
        user = databaseUri.userInfo.split(":")[0]
        password = databaseUri.userInfo.split(":")[1]
    }
    val usersDAO = UsersDAO(dataSource)
    val credentialsDataStore = CredentialsDataStore(usersDAO)
    val transport = NetHttpTransport()
    val jsonFactory = JacksonFactory()
    val googleAuthorizationCodeFlow = GoogleAuthorizationCodeFlow
            .Builder(
                    transport,
                    jsonFactory,
                    environment.config.property("google.clientId").getString(),
                    environment.config.property("google.clientSecret").getString(),
                    listOf(YouTubeScopes.YOUTUBE)
            )
            .setCredentialDataStore(credentialsDataStore)
            .build()
    val bot = telegramBot(environment.config.property("telegram.token").getString())

    routing {
        pages()
        webhook(usersDAO, googleAuthorizationCodeFlow, bot, transport, jsonFactory)
        oauth(googleAuthorizationCodeFlow)
    }
}
