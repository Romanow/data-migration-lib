package ru.romanow.migration.processors

import org.springframework.batch.item.ItemProcessor
import org.springframework.expression.spel.standard.SpelExpressionParser
import ru.romanow.migration.constansts.FieldMap
import ru.romanow.migration.properties.FieldOperation

class AdditionalFieldProcessorFactory : ProcessorFactory {
    private val parser = SpelExpressionParser()

    override fun create(field: FieldOperation): ItemProcessor<FieldMap, FieldMap> {
        val target = field.target!!
        return ItemProcessor {
            it[target.name!!] = parser.parseExpression(target.defaultValue!!).getValue(target.type)
            return@ItemProcessor it
        }
    }
}
