package ru.romanow.migration.processors

import org.springframework.batch.item.ItemProcessor
import org.springframework.core.convert.ConversionService
import org.springframework.expression.spel.standard.SpelExpressionParser
import ru.romanow.migration.properties.FieldMapping

class ModifyFieldsProcessor(
    private val additionalFields: List<FieldMapping>,
    private val conversionService: ConversionService
) : ItemProcessor<MutableMap<String, Any?>, MutableMap<String, Any?>> {
    private val parser = SpelExpressionParser()

    override fun process(item: MutableMap<String, Any?>): MutableMap<String, Any?> {
        for (field in additionalFields) {
            if (!conversionService.canConvert(field.sourceType, field.targetType)) {
                throw IllegalArgumentException(
                    "Can't convert source field ${field.sourceName} " +
                        "with type ${field.sourceType} to ${field.targetType}"
                )
            }
            val value = item[field.sourceName]
            item[field.targetName] =
                conversionService.convert(value, field.targetType) ?: buildDefault(field.defaultValue, field.targetType)
            item.remove(field.sourceName)
        }
        return item
    }

    private fun buildDefault(expression: String?, cls: Class<Any>): Any? {
        return if (expression != null) parser.parseExpression(expression).getValue(cls) else null
    }
}
