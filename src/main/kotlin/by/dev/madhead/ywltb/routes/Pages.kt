package by.dev.madhead.ywltb.routes

import io.ktor.http.content.defaultResource
import io.ktor.http.content.resources
import io.ktor.http.content.static
import io.ktor.routing.Route

fun Route.pages() {
    static("") {
        resources("static")
        defaultResource("static/index.html")
    }
}
