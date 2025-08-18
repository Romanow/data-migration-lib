package ru.romanow.migration.processors

import org.springframework.batch.item.ItemProcessor
import org.springframework.beans.factory.ListableBeanFactory
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.expression.BeanFactoryResolver
import org.springframework.core.convert.ConversionService
import org.springframework.expression.spel.standard.SpelExpressionParser
import org.springframework.expression.spel.support.StandardEvaluationContext
import org.springframework.stereotype.Component
import ru.romanow.migration.constansts.CONVERTOR_SERVICE_BEAN_NAME
import ru.romanow.migration.constansts.FieldMap
import ru.romanow.migration.constansts.MODIFY_FIELD_PROCESSOR_BEAN_NAME
import ru.romanow.migration.properties.FieldOperation

@Component(MODIFY_FIELD_PROCESSOR_BEAN_NAME)
class ModifyFieldProcessorFactory(
    private val beanFactory: ListableBeanFactory,
    @Qualifier(CONVERTOR_SERVICE_BEAN_NAME) private val conversionService: ConversionService
) : ProcessorFactory,
    JobContextAware {

    private val parser = SpelExpressionParser()
    private var jobParameters = mapOf<String, Any>()

    override fun notify(jobParameters: Map<String, Any>) {
        this.jobParameters = jobParameters
    }

    override fun create(field: FieldOperation): ItemProcessor<FieldMap, FieldMap> {
        val source = field.source!!
        val target = field.target!!
        return ItemProcessor {
            if (!conversionService.canConvert(source.type, target.type!!)) {
                throw IllegalArgumentException(
                    "Can't convert source field ${source.name} with type ${source.type} to ${target.type}"
                )
            }

            val value = conversionService.convert(it[source.name], target.type!!)
                ?: parseExpression(target.defaultValue)

            it[target.name!!] = value
            if (source.name != target.name) {
                it.remove(source.name)
            }
            return@ItemProcessor it
        }
    }

    private inline fun <reified T> parseExpression(expression: String?): T? {
        val context = StandardEvaluationContext().apply {
            beanResolver = BeanFactoryResolver(beanFactory)
            setVariable("jobParameters", jobParameters)
        }
        return if (expression != null) parser.parseExpression(expression).getValue(context, T::class.java) else null
    }
}
