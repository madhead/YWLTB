package by.dev.madhead.ywltb.entity

data class User(
        val id: Long,
        val accessToken: String,
        val refreshToken: String,
        val videosAdded: Int? = null
)
