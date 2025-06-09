package ru.romanow.migration.processors

import org.springframework.batch.item.ItemProcessor
import org.springframework.expression.spel.standard.SpelExpressionParser
import ru.romanow.migration.properties.FieldOperation

class AdditionalFieldProcessorFactory : ProcessorFactory {
    private val parser = SpelExpressionParser()

    override fun create(field: FieldOperation): ItemProcessor<MutableMap<String, Any?>, MutableMap<String, Any?>> {
        return ItemProcessor {
            it[field.target?.name!!] = parser.parseExpression(field.target?.defaultValue!!).getValue(field.target?.type)
            return@ItemProcessor it
        }
    }
}


