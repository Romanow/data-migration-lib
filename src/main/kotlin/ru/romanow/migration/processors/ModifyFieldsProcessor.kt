package ru.romanow.migration.processors

import org.springframework.batch.item.ItemProcessor
import org.springframework.expression.spel.standard.SpelExpressionParser
import ru.romanow.migration.properties.FieldMapping

class ModifyFieldsProcessor(
    private val additionalFields: List<FieldMapping>
) : ItemProcessor<MutableMap<String, Any>, MutableMap<String, Any>> {
    private val parser = SpelExpressionParser()

    override fun process(item: MutableMap<String, Any>): MutableMap<String, Any> {
        return item
    }
}
