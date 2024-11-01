import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import java.sql.Connection

interface Transactor {
    fun <R> transaction(block: (Connection) -> Result<R>): Result<R>
}

object Database : Transactor {
    private val pool: HikariDataSource = HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = "jdbc:sqlite::memory:"
            maximumPoolSize = 2
        })

    fun conn(): Connection {
        return pool.connection
    }

    override fun <R> transaction(block: (Connection) -> Result<R>): Result<R> {
        val connection = conn()

        return try {
            connection.autoCommit = false
            val result = block(connection)
            connection.commit()
            result
        } catch (e: Exception) {
            connection.rollback()
            Result.failure(e)
        } finally {
            connection.close()
        }
    }
}
