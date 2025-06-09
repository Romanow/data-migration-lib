package ru.romanow.migration.processors

import org.springframework.batch.item.ItemProcessor
import ru.romanow.migration.properties.FieldOperation

interface ProcessorFactory {
    fun create(field: FieldOperation): ItemProcessor<MutableMap<String, Any?>, MutableMap<String, Any?>>
}
