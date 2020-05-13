package by.dev.madhead.ywltb

import io.ktor.application.Application
import io.ktor.http.content.defaultResource
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.routing.routing

fun Application.pages() {
    routing {
        static("") {
            resources("static")
            defaultResource("static/index.html")
        }
    }
}
