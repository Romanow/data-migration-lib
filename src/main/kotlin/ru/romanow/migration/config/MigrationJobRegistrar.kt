package ru.romanow.migration.config

import org.slf4j.LoggerFactory
import org.springframework.batch.core.*
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
import ru.romanow.migration.constansts.MODIFY_FIELD_PROCESSOR_BEAN_NAME
import ru.romanow.migration.constansts.REMOVE_FIELD_PROCESSOR_BEAN_NAME
import ru.romanow.migration.processors.JobContextAware
import ru.romanow.migration.processors.JobContextListener
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
    private val defaultProcessor: ItemProcessor<FieldMap, FieldMap>,
    private val writer: ItemWriter<FieldMap>,
    private val processors: Map<String, ProcessorFactory>,
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val jobLauncher: JobLauncher
) : BeanFactoryPostProcessor {
    private val logger = LoggerFactory.getLogger(BeanFactoryPostProcessor::class.java)

    private val additionalFieldProcessorFactory = processors[ADDITIONAL_FIELD_PROCESSOR_BEAN_NAME]
        ?: throw IllegalStateException("Can't find $ADDITIONAL_FIELD_PROCESSOR_BEAN_NAME bean")
    private val modifyFieldProcessorFactory = processors[MODIFY_FIELD_PROCESSOR_BEAN_NAME]
        ?: throw IllegalStateException("Can't find $MODIFY_FIELD_PROCESSOR_BEAN_NAME bean")
    private val removeFieldsProcessorFactory = processors[REMOVE_FIELD_PROCESSOR_BEAN_NAME]
        ?: throw IllegalStateException("Can't find $REMOVE_FIELD_PROCESSOR_BEAN_NAME bean")

    override fun postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {
        for (table in properties.tables) {
            val jobContextAware = JobContextListener()
            processors.filterValues { it is JobContextAware }
                .forEach { (_, processor) -> jobContextAware.addListener(processor as JobContextAware) }

            val step = step(table.jobName, jobContextAware, configureProcessors(table.fields))
            val migrationJob = job(table.jobName, step)
            val runner = runner(migrationJob, table)
            val beanDefinition = genericBeanDefinition(BatchJobRunner::class.java) { runner }.beanDefinition
            (beanFactory as DefaultListableBeanFactory).registerBeanDefinition(table.jobName, beanDefinition)
        }
    }

    private fun configureProcessors(fields: List<FieldOperation>?): ItemProcessor<FieldMap, FieldMap> {
        val list = fields?.map {
            when (it.operation) {
                ADD -> additionalFieldProcessorFactory.create(it)
                MODIFY -> modifyFieldProcessorFactory.create(it)
                REMOVE -> removeFieldsProcessorFactory.create(it)
                CUSTOM -> processors[it.processor]?.create(it)!!
            }
        } ?: listOf()
        return if (list.isNotEmpty()) CompositeItemProcessor(list) else defaultProcessor
    }

    private fun step(
        name: String, jobContextListener: JobContextListener, processor: ItemProcessor<FieldMap, FieldMap>
    ): Step =
        StepBuilder("$name-step", jobRepository)
            .chunk<FieldMap, FieldMap>(properties.chunkSize, transactionManager)
            .listener(jobContextListener)
            .reader(reader)
            .processor(processor)
            .writer(writer)
            .build()

    private fun job(name: String, step: Step): Job = JobBuilder(name, jobRepository).start(step).build()

    private fun runner(migrationJob: Job, table: TableMigration): BatchJobRunner {
        return object : BatchJobRunner {
            override fun run(context: Map<String, Any>) {
                val source = table.source
                val target = table.target
                val params = JobParametersBuilder()
                    .addString("key", DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now()))
                    .addString("keyColumnName", table.keyColumnName)
                    .addString("sourceTable", source.schema + "." + source.table)
                    .addString("targetTable", target.schema + "." + target.table)
                    .addJobParameters(JobParameters(context.mapValues { JobParameter(it.value, it.value.javaClass) }))
                    .toJobParameters()

                val execution = jobLauncher.run(migrationJob, params)
                if (execution.status == BatchStatus.COMPLETED) {
                    logger.info(
                        "Migration process '{}' from '{}' to '{}' completed successfully (duration: {})",
                        table.jobName, "${source.schema}.${source.table}", "${target.schema}.${target.table}",
                        Duration.between(execution.startTime?.toLocalTime(), execution.endTime?.toLocalTime())
                    )
                } else {
                    logger.error(
                        "Migration process '{}' from '{}' to '{}' failed with status {}",
                        table.jobName, "${source.schema}.${source.table}",
                        "${target.schema}.${target.table}", execution.status
                    )
                }
            }
        }
    }
}
