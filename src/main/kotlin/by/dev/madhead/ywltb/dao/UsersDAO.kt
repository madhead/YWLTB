package by.dev.madhead.ywltb.dao

import by.dev.madhead.ywltb.entity.User
import java.sql.Types
import javax.sql.DataSource

class UsersDAO(private val dataSource: DataSource) {
    fun getById(id: Int): User? {
        dataSource.connection.use { connection ->
            connection
                    .prepareStatement("SELECT * FROM users WHERE id = ?;")
                    .use { preparedStatement ->
                        preparedStatement.setInt(1, id)
                        preparedStatement.executeQuery().use { resultSet ->
                            if (resultSet.next()) {
                                return User(
                                        id = resultSet.getInt(1),
                                        accessToken = resultSet.getString(2),
                                        refreshToken = resultSet.getString(3),
                                        videosAdded = run {
                                            val value = resultSet.getInt(4)

                                            if (resultSet.wasNull()) {
                                                null
                                            } else {
                                                value
                                            }
                                        }
                                )
                            } else {
                                return null
                            }
                        }
                    }
        }
    }

    fun save(user: User) {
        dataSource.connection.use { connection ->
            connection
                    .prepareStatement("""
                        INSERT INTO users
                        VALUES (?, ?, ?, ?)
                        ON CONFLICT (id)
                            DO UPDATE
                            SET id=EXCLUDED.id,
                                accesstoken = EXCLUDED.accesstoken,
                                refreshtoken = EXCLUDED.refreshtoken,
                                videosadded = EXCLUDED.videosadded;
                    """.trimIndent())
                    .use { preparedStatement ->
                        preparedStatement.setInt(1, user.id)
                        preparedStatement.setString(2, user.accessToken)
                        preparedStatement.setString(3, user.refreshToken)
                        if (user.videosAdded != null) {
                            preparedStatement.setInt(4, user.videosAdded)
                        } else {
                            preparedStatement.setNull(4, Types.INTEGER)
                        }
                        preparedStatement.executeUpdate()
                    }
        }
    }

    fun trackVideosAdded(id: Int, videosAdded: Int) {
        dataSource.connection.use { connection ->
            connection
                    .prepareStatement("""
                        UPDATE users
                        SET videosadded = coalesce(videosadded, 0) + ?
                        WHERE id = ?
                    """.trimIndent())
                    .use { preparedStatement ->
                        preparedStatement.setInt(1, videosAdded)
                        preparedStatement.setInt(2, id)
                        preparedStatement.executeUpdate()
                    }
        }
    }
}
