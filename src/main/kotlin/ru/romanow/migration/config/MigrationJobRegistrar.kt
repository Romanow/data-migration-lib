package ru.romanow.migration.config

import org.slf4j.LoggerFactory
import org.springframework.batch.core.BatchStatus
import org.springframework.batch.core.Job
import org.springframework.batch.core.JobParametersBuilder
import org.springframework.batch.core.Step
import org.springframework.batch.core.job.builder.JobBuilder
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.core.step.builder.StepBuilder
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.ItemWriter
import org.springframework.batch.item.support.CompositeItemProcessor
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.transaction.PlatformTransactionManager
import ru.romanow.migration.constansts.ADDITIONAL_FIELD_PROCESSOR_BEAN_NAME
import ru.romanow.migration.constansts.FieldMap
import ru.romanow.migration.constansts.REMOVE_FIELD_PROCESSOR_BEAN_NAME
import ru.romanow.migration.processors.ProcessorFactory
import ru.romanow.migration.properties.FieldOperation
import ru.romanow.migration.properties.MigrationProperties
import ru.romanow.migration.properties.OperationType.*
import ru.romanow.migration.properties.TableMigration
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class MigrationJobRegistrar(
    private val properties: MigrationProperties,
    private val reader: ItemReader<FieldMap>,
    private val processor: ItemProcessor<FieldMap, FieldMap>,
    private val writer: ItemWriter<FieldMap>,
    private val processors: Map<String, ProcessorFactory>,
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val jobLauncher: JobLauncher
) : BeanFactoryPostProcessor {
    private val logger = LoggerFactory.getLogger(BeanFactoryPostProcessor::class.java)

    private val additionalFieldProcessorFactory = processors[ADDITIONAL_FIELD_PROCESSOR_BEAN_NAME]
        ?: throw IllegalStateException("Can't find $ADDITIONAL_FIELD_PROCESSOR_BEAN_NAME bean")
    private val modifyFieldProcessorFactory = processors[ADDITIONAL_FIELD_PROCESSOR_BEAN_NAME]
        ?: throw IllegalStateException("Can't find $ADDITIONAL_FIELD_PROCESSOR_BEAN_NAME bean")
    private val removeFieldsProcessorFactory = processors[REMOVE_FIELD_PROCESSOR_BEAN_NAME]
        ?: throw IllegalStateException("Can't find $REMOVE_FIELD_PROCESSOR_BEAN_NAME bean")

    override fun postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {
        for (table in properties.tables) {
            val step = step(table.name, configureProcessors(table.fields))
            val migrationJob = job(table.name, step)
            val runner = runner(migrationJob, table)
            val beanDefinition = genericBeanDefinition(Runnable::class.java) { runner }.beanDefinition
            (beanFactory as DefaultListableBeanFactory).registerBeanDefinition(table.name, beanDefinition)
        }
    }

    private fun configureProcessors(fields: List<FieldOperation>?): ItemProcessor<FieldMap, FieldMap> {
        val list = mutableListOf<ItemProcessor<FieldMap, FieldMap>>()
        fields?.forEach {
            when (it.operation) {
                ADD -> list.add(additionalFieldProcessorFactory.create(it))
                MODIFY -> list.add(modifyFieldProcessorFactory.create(it))
                REMOVE -> list.add(removeFieldsProcessorFactory.create(it))
                CUSTOM -> list.add(processors[it.processor]?.create(it)!!)
            }
        }
        return if (list.isNotEmpty()) CompositeItemProcessor(list) else processor
    }

    private fun step(name: String, processor: ItemProcessor<FieldMap, FieldMap>): Step =
        StepBuilder("$name-step", jobRepository)
            .chunk<FieldMap, FieldMap>(properties.chunkSize, transactionManager)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build()

    private fun job(name: String, step: Step): Job = JobBuilder(name, jobRepository).start(step).build()

    private fun runner(migrationJob: Job, table: TableMigration): Runnable {
        return Runnable {
            val source = table.source
            val target = table.target
            val params = JobParametersBuilder()
                .addString("key", DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now()))
                .addString("keyColumnName", table.keyColumnName)
                .addString("sourceTable", source.schema + "." + source.table)
                .addString("targetTable", target.schema + "." + target.table)
                .toJobParameters()

            val execution = jobLauncher.run(migrationJob, params)
            if (execution.status == BatchStatus.COMPLETED) {
                logger.info(
                    "Migration process '{}' from '{}' to '{}' completed successfully (duration: {})",
                    table.name, "${source.schema}.${source.table}", "${target.schema}.${target.table}",
                    Duration.between(execution.endTime?.toLocalTime(), execution.startTime?.toLocalTime())
                )
            } else {
                logger.error(
                    "Migration process '{}' from '{}' to '{}' failed with status {}",
                    table.name, "${source.schema}.${source.table}",
                    "${target.schema}.${target.table}", execution.status
                )
            }
        }
    }
}
