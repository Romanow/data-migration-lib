package ru.romanow.migration.writer

import org.springframework.batch.item.Chunk
import org.springframework.batch.item.database.JdbcBatchItemWriter
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import ru.romanow.migration.constansts.FieldMap
import javax.sql.DataSource

class DynamicJdbcBatchItemWriter(private val tableName: String?, dataSource: DataSource) :
    JdbcBatchItemWriter<FieldMap>() {

    init {
        sql = "INSERT INTO $tableName (id) VALUES :id"
        namedParameterJdbcTemplate = NamedParameterJdbcTemplate(dataSource)
    }

    override fun write(chunk: Chunk<out FieldMap>) {
        sql = generateSql(tableName, chunk.items)
        super.write(chunk)
    }

    private fun generateSql(targetTableName: String?, items: List<FieldMap>): String {
        val item = items.first()
        val fields = item.keys.joinToString(separator = ", ")
        val values = item.keys.joinToString(separator = ", ") { ":$it" }
        return "INSERT INTO $targetTableName($fields) VALUES ($values)"
    }
}
