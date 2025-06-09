package ru.romanow.migration.processors

import org.springframework.batch.item.ItemProcessor
import ru.romanow.migration.properties.FieldOperation

class RemoveFieldProcessorFactory : ProcessorFactory {
    override fun create(field: FieldOperation): ItemProcessor<MutableMap<String, Any?>, MutableMap<String, Any?>> {
        return ItemProcessor {
            it.remove(field.source?.name)
            return@ItemProcessor it
        }
    }
}
