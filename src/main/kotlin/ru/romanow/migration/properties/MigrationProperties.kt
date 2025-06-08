package ru.romanow.migration.properties

import org.springframework.boot.context.properties.ConfigurationProperties
import java.lang.reflect.Type

@ConfigurationProperties(prefix = "migration")
data class MigrationProperties(
    var chunkSize: Int = 5000,
    var jobs: List<MigrationJob>
)

data class MigrationJob(
    var name: String,
    var tables: List<Tables>,
)

data class Tables(
    val keyColumnName: String,
    val source: Table,
    val target: Table,
    var mapping: List<FieldMapping>? = null,
    var additionalFields: List<FieldDeclaration>? = null,
    var ignoreFields: List<String>? = null,
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
    var sourceType: Type? = null,
    var targetName: String,
    var targetType: Type? = null,
    var defaultValue: String? = null
)
