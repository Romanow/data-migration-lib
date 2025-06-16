package ru.romanow.migration.processors

import org.springframework.batch.item.ItemProcessor
import ru.romanow.migration.constansts.FieldMap
import ru.romanow.migration.properties.FieldOperation

class RemoveFieldProcessorFactory : ProcessorFactory {
    override fun create(field: FieldOperation): ItemProcessor<FieldMap, FieldMap> {
        val source = field.source!!
        return ItemProcessor {
            it.remove(source.name)
            return@ItemProcessor it
        }
    }
}
