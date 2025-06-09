package ru.romanow.migration.processors

import org.springframework.batch.item.ItemProcessor
import org.springframework.core.convert.ConversionService
import org.springframework.expression.spel.standard.SpelExpressionParser
import ru.romanow.migration.properties.FieldOperation

class ModifyFieldProcessorFactory(private val conversionService: ConversionService) : ProcessorFactory {
    private val parser = SpelExpressionParser()

    override fun create(field: FieldOperation): ItemProcessor<MutableMap<String, Any?>, MutableMap<String, Any?>> {
        return ItemProcessor {
            if (!conversionService.canConvert(field.source?.type, field.target?.type!!)) {
                throw IllegalArgumentException(
                    "Can't convert source field ${field.source?.name} " +
                        "with type ${field.source?.type} to ${field.target?.type}"
                )
            }
            val value = it[field.source?.name]
            it[field.target?.name!!] = conversionService.convert(value, field.target?.type!!)
                ?: buildDefault(field.target?.defaultValue, field.target?.type!!)
            it.remove(field.source?.name)
            return@ItemProcessor it
        }
    }

    private fun buildDefault(expression: String?, cls: Class<Any>): Any? {
        return if (expression != null) parser.parseExpression(expression).getValue(cls) else null
    }
}
