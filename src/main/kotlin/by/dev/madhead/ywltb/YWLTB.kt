package by.dev.madhead.ywltb

import by.dev.madhead.telek.model.Update
import com.github.insanusmokrassar.TelegramBotAPI.extensions.api.send.sendMessage
import com.github.insanusmokrassar.TelegramBotAPI.extensions.api.telegramBot
import com.github.insanusmokrassar.TelegramBotAPI.types.toChatId
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.Compression
import io.ktor.features.DefaultHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.request.receiveText
import io.ktor.response.respond
import io.ktor.routing.post
import io.ktor.routing.routing
import io.ktor.util.KtorExperimentalAPI
import kotlinx.serialization.ImplicitReflectionSerializer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import kotlinx.serialization.parse

@ImplicitReflectionSerializer
@KtorExperimentalAPI
fun Application.webhook() {
    install(DefaultHeaders)
    install(Compression)
    install(CallLogging)

    routing {
        val json = Json(configuration = JsonConfiguration.Stable.copy(encodeDefaults = false, ignoreUnknownKeys = true))
        val bot = telegramBot(environment.config.property("telegram.token").getString())

        post(environment.config.property("telegram.token").getString()) {
            val update = json.parse<Update>(call.receiveText())

            update.message?.let { message ->
                val links = message
                        .entities
                        ?.let { entities ->
                            entities
                                    .filter { (it.type == "text_link") || (it.type == "url") }
                                    .mapNotNull {
                                        when (it.type) {
                                            "text_link" -> it.url
                                            "url" -> message.text?.substring(it.offset, it.offset + it.length)
                                            else -> throw IllegalStateException("Unexpected entity type: ${it.type}")
                                        }
                                    }
                        }

                bot.sendMessage(
                        chatId = message.chat.id.toChatId(),
                        text = "Detected links: $links"
                )
            }

            call.respond(HttpStatusCode.OK)
        }
    }
}
