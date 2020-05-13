package by.dev.madhead.ywltb

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.features.CallLogging
import io.ktor.features.Compression
import io.ktor.features.DefaultHeaders
import io.ktor.response.respond
import io.ktor.routing.get
import io.ktor.routing.routing

fun Application.main() {
    install(DefaultHeaders)
    install(Compression)
    install(CallLogging)

    routing {
        get("/") {
            call.respond("Hello, world!")
        }
    }
}
