package by.dev.madhead.ywltb.dao

import by.dev.madhead.ywltb.entity.User
import com.google.api.client.auth.oauth2.StoredCredential
import com.google.api.client.util.store.DataStore

class CredentialsDataStore(
        private val usersDAO: UsersDAO
) : DataStore<StoredCredential> {
    override fun getDataStoreFactory() = throw UnsupportedOperationException()

    override fun clear() = throw UnsupportedOperationException()

    override fun isEmpty() = throw UnsupportedOperationException()

    override fun containsValue(value: StoredCredential?) = throw UnsupportedOperationException()

    override fun getId() = "YWLTB"

    override fun set(key: String, value: StoredCredential): DataStore<StoredCredential> {
        val id = key.toLongOrNull() ?: throw IllegalArgumentException()
        val user = usersDAO
                .getById(id)
                ?.copy(accessToken = value.accessToken, refreshToken = value.refreshToken)
                ?: User(id.toLong(), value.accessToken, value.refreshToken)

        usersDAO.save(user)

        return this
    }

    override fun values() = throw UnsupportedOperationException()

    override fun size() = throw UnsupportedOperationException()

    override fun keySet() = throw UnsupportedOperationException()

    override fun containsKey(key: String?) = throw UnsupportedOperationException()

    override fun get(key: String): StoredCredential? {
        val id = key.toLongOrNull() ?: throw IllegalArgumentException()
        val user = usersDAO.getById(id) ?: return null

        return StoredCredential().apply {
            accessToken = user.accessToken
            refreshToken = user.refreshToken
        }
    }

    override fun delete(key: String?) = throw UnsupportedOperationException()
}
