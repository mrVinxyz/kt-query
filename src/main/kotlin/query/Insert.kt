package query

import java.sql.Connection
import java.sql.Statement

/**
 * The `Insert` class is responsible for building SQL INSERT statements for a given table.
 *
 * @property table the table for which the INSERT statement will be created
 * @property insertColumns a list of columns to be inserted
 * @property argsValues a list of values to be inserted into the columns
 */
class Insert(val table: Table) {
    /**
     * Represents a mutable list of columns to be inserted into a table.
     *
     * This variable holds the list of columns that will be inserted into a [Table]. It is a
     * MutableList of [Column] objects. The type parameter of [Column] can be any type of data
     * stored in the column.
     */
    private val insertColumns = mutableListOf<Column<*>>()


    /**
     * The `argsValues` property is a mutable list of type `Any`. It is used to store the values that will be used as arguments
     * in an SQL INSERT statement.
     */
    private val argsValues = mutableListOf<Any>()

    /**
     * Returns an `Insert` object that can be used to construct an SQL INSERT statement for the
     * specified table. The `Insert` object provides various methods to specify the columns and
     * values to be inserted.
     *
     * @param init The initialization block that takes an `Insert` object as a parameter. This
     *   block is used to set the columns and values for the insertion.
     * @return An `Insert` object.
     */
    fun insert(init: (Insert) -> Unit): Insert {
        return Insert(table).apply(init)
    }

    /**
     * Inserts the given columns into the current Insert instance.
     *
     * @param column the columns to insert
     * @return the current Insert instance
     */
    fun insert(vararg column: Column<*>): Insert {
        insertColumns.addAll(column)
        return this
    }

    /**
     * Sets a column with the specified value in the [Insert].
     *
     * @param column the column to set the value for
     * @param value the value to set for the column
     * @return the [Insert] object
     */
    operator fun <T> set(column: Column<*>, value: T?): Insert {
        value?.let { v ->
            insertColumns.add(column)
            argsValues.add(v)
        }

        return this
    }

    /**
     * Inserts values into the table.
     *
     * @param value the values to be inserted into the table as varargs
     * @return an instance of Insert for method chaining
     */
    fun values(vararg value: Any?): Insert {
        val nullColumns = mutableListOf<Column<*>>()

        value.forEachIndexed { index, any ->
            if (any != null) argsValues.add(any) else nullColumns.add(insertColumns[index])
        }

        insertColumns.removeAll(nullColumns)

        return this
    }

    /**
     * Retrieves the column values from a given map and adds them to the `argsValues` list. Only the
     * values corresponding to the columns in the `insertColumns` list are added.
     *
     * @param map a map containing column names and their corresponding values
     * @return the Insert object for method chaining
     */
    fun values(map: Map<String, Any?>): Insert {

        insertColumns.forEach { column ->
            map[column.key()]?.let { value -> value.let { argsValues.add(value) } }
        }

        return this
    }

    /**
     * Generates SQL arguments for an insert statement.
     *
     * @return a Pair containing the SQL statement and a list of argument values
     */
    fun sqlArgs(): Pair<String, List<Any>> {
        val sql = StringBuilder()

        sql.append("INSERT INTO ")
        sql.append(table.name())
        sql.append(" (")

        insertColumns.forEachIndexed { index, column ->
            sql.append(column.key())
            if (index < insertColumns.size - 1) sql.append(", ")
        }

        sql.append(") VALUES (")
        sql.append(insertColumns.joinToString(", ") { "?" })
        sql.append(")")

        return Pair(sql.toString(), argsValues)
    }
}

/**
 * Persists the data represented by the current `Insert` object in the given database connection.
 *
 * @param conn the database connection to use for the insert operation
 * @return a Result object that contains the generated id if the insert operation was successful, or the corresponding exception if it failed
 */
fun Insert.persist(conn: Connection): Result<Int> {
    return runCatching {
        val (sql, args) = sqlArgs()
        val generatedId = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS).use { stmt ->
            stmt.setParameters(args)
            stmt.executeUpdate()
            stmt.generatedKeys.getInt(1)
        }

        generatedId
    }
}

