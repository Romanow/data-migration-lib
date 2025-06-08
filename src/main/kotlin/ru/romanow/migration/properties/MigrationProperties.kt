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
    var modifyFields: List<FieldMapping>? = null,
    var additionalFields: List<FieldDeclaration>? = null,
    var removeFields: List<String>? = null,
)

data class Table(
    val table: String,
    val schema: String
)

data class FieldDeclaration(
    val name: String,
    var type: Class<Any>,
    var value: String
)

data class FieldMapping(
    var sourceName: String,
    var sourceType: Class<Any>,
    var targetName: String,
    var targetType: Class<Any>,
    var defaultValue: String? = null
)
