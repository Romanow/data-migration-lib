package ru.romanow.migration.processors

import org.springframework.batch.item.ItemProcessor
import org.springframework.beans.factory.ListableBeanFactory
import org.springframework.context.expression.BeanFactoryResolver
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import ru.romanow.migration.constansts.FieldMap
import ru.romanow.migration.properties.FieldOperation

class AdditionalFieldProcessorFactory(private val beanFactory: ListableBeanFactory) : ProcessorFactory {
    private val parser = SpelExpressionParser()

    override fun create(field: FieldOperation, jobContextAware: JobContextAware?): ItemProcessor<FieldMap, FieldMap> {
        val target = field.target!!
        return ItemProcessor {
            it[target.name!!] = parseExpression(target.defaultValue, jobContextAware?.jobParameters)
            return@ItemProcessor it
        }
    }

    private inline fun <reified T> parseExpression(expression: String?, jobParameters: Map<String, Any>?): T? {
        val context = StandardEvaluationContext().apply {
            beanResolver = BeanFactoryResolver(beanFactory)
            setVariable("jobParameters", jobParameters)
        }
        return if (expression != null) parser.parseExpression(expression).getValue(context, T::class.java) else null
    }
}
