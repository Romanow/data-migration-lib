package ru.romanow.migration.processors

import org.springframework.batch.item.ItemProcessor
import org.springframework.core.convert.ConversionService
import org.springframework.expression.spel.standard.SpelExpressionParser
import ru.romanow.migration.constansts.FieldMap
import ru.romanow.migration.properties.FieldOperation

class ModifyFieldProcessorFactory(private val conversionService: ConversionService) : ProcessorFactory {
    private val parser = SpelExpressionParser()

    override fun create(field: FieldOperation, jobContextAware: JobContextAware?): ItemProcessor<FieldMap, FieldMap> {
        val source = field.source!!
        val target = field.target!!
        return ItemProcessor {
            if (!conversionService.canConvert(source.type, target.type!!)) {
                throw IllegalArgumentException(
                    "Can't convert source field ${source.name} with type ${source.type} to ${target.type}"
                )
            }
            val value = it[source.name]
            it[target.name!!] =
                conversionService.convert(value, target.type!!) ?: buildDefault(target.defaultValue)
            it.remove(source.name)
            return@ItemProcessor it
        }
    }

    private inline fun <reified T> buildDefault(expression: String?): T? {
        return if (expression != null) parser.parseExpression(expression).getValue(T::class.java) else null
    }
}
