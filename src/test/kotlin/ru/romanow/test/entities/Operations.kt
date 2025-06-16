package ru.romanow.test.entities

import ru.romanow.migration.properties.OperationType
import java.time.LocalDateTime
import java.util.*

data class Operations(
    val processId: UUID? = null,
    val solveId: UUID? = null,
    val type: OperationType? = null,
    val startedAt: LocalDateTime? = null,
    val startedBy: String? = null
)
