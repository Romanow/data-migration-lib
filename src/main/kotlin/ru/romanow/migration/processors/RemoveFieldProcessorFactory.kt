package ru.romanow.migration.processors

import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component
import ru.romanow.migration.constansts.FieldMap
import ru.romanow.migration.constansts.REMOVE_FIELD_PROCESSOR_BEAN_NAME
import ru.romanow.migration.properties.FieldOperation

@Component(REMOVE_FIELD_PROCESSOR_BEAN_NAME)
class RemoveFieldProcessorFactory : ProcessorFactory {
    override fun create(field: FieldOperation): ItemProcessor<FieldMap, FieldMap> {
        val source = field.source!!
        return ItemProcessor {
            it.remove(source.name)
            return@ItemProcessor it
        }
    }
}
