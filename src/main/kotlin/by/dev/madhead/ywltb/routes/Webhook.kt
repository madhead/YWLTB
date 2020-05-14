package by.dev.madhead.ywltb.routes

import by.dev.madhead.telek.model.Message
import by.dev.madhead.telek.model.Update
import by.dev.madhead.telek.model.User
import by.dev.madhead.ywltb.dao.UsersDAO
import com.github.insanusmokrassar.TelegramBotAPI.bot.RequestsExecutor
import com.github.insanusmokrassar.TelegramBotAPI.extensions.api.send.sendMessage
import com.github.insanusmokrassar.TelegramBotAPI.types.ParseMode.MarkdownV2
import com.github.insanusmokrassar.TelegramBotAPI.types.toChatId
import com.github.insanusmokrassar.TelegramBotAPI.utils.extensions.escapeMarkdownV2Common
import com.github.insanusmokrassar.TelegramBotAPI.utils.extensions.escapeMarkdownV2Link
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.JsonFactory
import com.google.api.services.youtube.YouTube
import com.google.api.services.youtube.model.PlaylistItem
import com.google.api.services.youtube.model.PlaylistItemSnippet
import com.google.api.services.youtube.model.ResourceId
import io.ktor.application.call
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.application
import io.ktor.routing.post
import io.ktor.util.KtorExperimentalAPI
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.UnstableDefault
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.parse
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.text.MessageFormat
import java.util.Locale
import java.util.ResourceBundle

@ImplicitReflectionSerializer
@UnstableDefault
@KtorExperimentalAPI
fun Route.webhook(
        usersDAO: UsersDAO,
        authorizationCodeFlow: GoogleAuthorizationCodeFlow,
        bot: RequestsExecutor,
        transport: HttpTransport,
        jsonFactory: JsonFactory
) {
    val logger = LogManager.getLogger("by.dev.madhead.ywltb.routes.Webhook")
    val json = Json(JsonConfiguration(
            encodeDefaults = false,
            ignoreUnknownKeys = true
    ))
    val redirectUrl = application.environment.config.property("google.redirectUrl").getString()

    post(application.environment.config.property("telegram.token").getString()) {
        val payload = call.receiveText()

        logger.debug("Request payload: {}", payload)

        val update = json.parse<Update>(payload)

        logger.info("Parsed update: {}", update)

        val message = update.message ?: return@post
        val user = message.from ?: return@post

        if (message.text in listOf("/start", "/login")) {
            logger.debug("Starting explicit login flow")

            startExplicitLoginFlow(user, bot, authorizationCodeFlow, redirectUrl)
        } else {
            val credentials = authorizationCodeFlow.loadCredential(user.id.toString())

            if (credentials != null) {
                logger.debug("Processing YouTube links")

                processYouTubeLinks(message, user, bot, usersDAO, credentials, transport, jsonFactory, logger)
            } else {
                logger.debug("Login required")

                loginRequired(user, bot, authorizationCodeFlow, redirectUrl)
            }
        }
        call.respond(HttpStatusCode.OK)
    }
}

@KtorExperimentalAPI
private suspend fun startExplicitLoginFlow(
        user: User,
        bot: RequestsExecutor,
        authorizationCodeFlow: GoogleAuthorizationCodeFlow,
        redirectUrl: String
) {
    val locale = user.languageCode?.let { Locale(it) } ?: Locale.ENGLISH
    val messages = ResourceBundle.getBundle("messages", locale)
    val credentials = authorizationCodeFlow.loadCredential(user.id.toString())

    if (credentials == null) {
        loginRequired(user, bot, authorizationCodeFlow, redirectUrl)
    } else {
        bot.sendMessage(
                chatId = user.id.toChatId(),
                text = MessageFormat(messages.getString("login.exists"), locale).format(emptyArray<Any>()),
                parseMode = MarkdownV2
        )
    }
}

@KtorExperimentalAPI
private suspend fun loginRequired(
        user: User,
        bot: RequestsExecutor,
        authorizationCodeFlow: GoogleAuthorizationCodeFlow,
        redirectUrl: String
) {
    val locale = user.languageCode?.let { Locale(it) } ?: Locale.ENGLISH
    val messages = ResourceBundle.getBundle("messages", locale)
    val authorizationUrl = authorizationCodeFlow
            .newAuthorizationUrl()
            .setRedirectUri(redirectUrl)
            .setState(user.id.toString())
            .setAccessType("offline")
            .setApprovalPrompt("force")

    bot.sendMessage(
            chatId = user.id.toChatId(),
            text = MessageFormat(messages.getString("login"), locale).format(arrayOf<Any>(authorizationUrl.build())),
            parseMode = MarkdownV2
    )
}

private suspend fun processYouTubeLinks(
        message: Message,
        user: User,
        bot: RequestsExecutor,
        usersDAO: UsersDAO,
        credentials: Credential,
        googleHttpTransport: HttpTransport,
        googleJsonFactory: JsonFactory,
        logger: Logger
) {
    message
            .entities
            ?.let { entities ->
                entities
                        .mapNotNull {
                            when (it.type) {
                                "text_link" -> it.url
                                "url" -> message.text?.substring(it.offset, it.offset + it.length)
                                else -> null
                            }
                        }
                        .mapNotNull {
                            it.youTubeVideoId
                        }
            }
            ?.takeIf { it.isNotEmpty() }
            ?.let { youTubeVideos ->
                val youTube = YouTube.Builder(
                        googleHttpTransport,
                        googleJsonFactory,
                        credentials
                ).build()

                youTubeVideos.forEach { id ->
                    try {
                        youTube
                                .PlaylistItems()
                                .insert(
                                        "snippet",
                                        PlaylistItem()
                                                .setSnippet(
                                                        PlaylistItemSnippet()
                                                                .setPlaylistId("WL")
                                                                .setResourceId(
                                                                        ResourceId()
                                                                                .setKind("youtube#video")
                                                                                .setVideoId(id)
                                                                )
                                                )

                                )
                                .execute()
                    } catch (e: Exception) {
                        logger.error("Failed to insert a video", e)
                    }
                }

                try {
                    usersDAO.trackVideosAdded(user.id, youTubeVideos.size)
                } catch (e: Exception) {
                    logger.error("Failed update statistics", e)
                }

                val locale = user.languageCode?.let { Locale(it) } ?: Locale.ENGLISH
                val messages = ResourceBundle.getBundle("messages", locale)

                try {
                    bot.sendMessage(
                            chatId = user.id.toChatId(),
                            disableWebPagePreview = true,
                            replyToMessageId = message.messageId.toLong(),
                            text = MessageFormat(messages.getString("added"), locale).format(arrayOf<Any>(
                                    youTubeVideos.joinToString(separator = "\n", prefix = "\n") {
                                        "• [${it.escapeMarkdownV2Common()}](http://youtu.be/${it.escapeMarkdownV2Link()})"
                                    }
                            )),
                            parseMode = MarkdownV2
                    )
                } catch (e: Exception) {
                    logger.error("Failed to send message", e)
                }
            }
}

private val youTubeLinkRegex = "^.*((youtu.be\\/)|(v\\/)|(\\/u\\/\\w\\/)|(embed\\/)|(watch\\?))\\??v?=?([^#\\&\\?]*).*".toRegex()
private val String.youTubeVideoId: String?
    get() {
        val (_, _, _, _, _, _, id) = youTubeLinkRegex.matchEntire(this)?.destructured ?: return null

        return id
    }
