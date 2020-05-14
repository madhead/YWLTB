package by.dev.madhead.ywltb.routes

import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow
import io.ktor.application.call
import io.ktor.response.respond
import io.ktor.routing.Route
import io.ktor.routing.application
import io.ktor.routing.get
import io.ktor.util.KtorExperimentalAPI
import org.apache.logging.log4j.LogManager

@KtorExperimentalAPI
fun Route.oauth(authorizationCodeFlow: GoogleAuthorizationCodeFlow) {
    val logger = LogManager.getLogger("by.dev.madhead.ywltb.routes.OAuth")
    val redirectUrl = application.environment.config.property("google.redirectUrl").getString()

    get("authorize") {
        val userId = call.request.queryParameters["state"]
        val code = call.request.queryParameters["code"]

        logger.debug("OAuth callback for {}: {}", userId, code)

        if ((userId != null) && (code != null)) {
            val tokenResponse = authorizationCodeFlow
                    .newTokenRequest(code)
                    .setRedirectUri(redirectUrl)
                    .execute()

            authorizationCodeFlow.createAndStoreCredential(tokenResponse, userId)
        }

        call.respond("Close this window now.")
    }
}
