package ru.romanow.migration.processors

import org.springframework.batch.item.ItemProcessor
import ru.romanow.migration.constansts.FieldMap
import ru.romanow.migration.properties.FieldOperation

interface ProcessorFactory {
    fun create(field: FieldOperation): ItemProcessor<FieldMap, FieldMap>
}
