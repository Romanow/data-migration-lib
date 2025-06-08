package ru.romanow.migration.processors

import org.springframework.batch.item.ItemProcessor
import org.springframework.expression.spel.standard.SpelExpressionParser
import ru.romanow.migration.properties.FieldDeclaration

class AdditionalFieldsProcessor(
    private val additionalFields: List<FieldDeclaration>
) : ItemProcessor<MutableMap<String, Any>, MutableMap<String, Any>> {
    private val parser = SpelExpressionParser()

    override fun process(item: MutableMap<String, Any>): MutableMap<String, Any> {
        additionalFields.forEach { item[it.name] = parser.parseExpression(it.value).getValue(it.type)!! }
        return item
    }
}
