package ru.romanow.test.entities

import java.time.LocalDateTime
import java.util.*

data class Users(
    val uid: UUID? = null,
    val solveId: UUID? = null,
    val name: String? = null,
    val age: Int? = null,
    val location: String? = null,
    val createdDate: LocalDateTime? = null,
    val checksum: String? = null
)
