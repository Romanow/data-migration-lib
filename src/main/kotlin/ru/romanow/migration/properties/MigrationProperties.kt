package ru.romanow.migration.properties

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "migration")
data class MigrationProperties(
    var chunkSize: Int = 5000,
    var tables: List<TableMigration>
)

data class TableMigration(
    var name: String,
    val keyColumnName: String,
    val source: Table,
    val target: Table,
    var fields: List<FieldOperation>? = null,
)

data class Table(
    val table: String,
    val schema: String
)

data class FieldOperation(
    val operation: OperationType,
    var source: FieldDeclaration? = null,
    var target: FieldDeclaration? = null,
    var processor: String? = null
)

data class FieldDeclaration(
    var name: String? = null,
    var type: Class<Any>? = null,
    var defaultValue: String? = null
)

enum class OperationType {
    ADD,
    MODIFY,
    REMOVE,
    CUSTOM
}
