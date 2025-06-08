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
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.config.BeanFactoryPostProcessor
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory
import org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.transaction.PlatformTransactionManager
import ru.romanow.migration.properties.MigrationProperties
import ru.romanow.migration.properties.Tables
import java.time.Duration
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class JobBeanRegistrar(
    private val properties: MigrationProperties,
    private val jobRepository: JobRepository,
    private val transactionManager: PlatformTransactionManager,
    private val sourceReader: ItemReader<Map<String, Any>>,
    private val targetWriter: ItemWriter<Map<String, Any>>,
    private val jobLauncher: JobLauncher
) : BeanFactoryPostProcessor {
    private val logger = LoggerFactory.getLogger(javaClass)

    override fun postProcessBeanFactory(beanFactory: ConfigurableListableBeanFactory) {
        for (job in properties.jobs) {
            val step = step(job.name)
            val migrationJob = job(job.name, step)
            val runner = runner(migrationJob, job.tables)
            val beanDefinition = genericBeanDefinition(Runnable::class.java) { runner }.beanDefinition
            (beanFactory as DefaultListableBeanFactory).registerBeanDefinition(job.name, beanDefinition)
        }
    }

    private fun step(name: String): Step =
        StepBuilder("$name-step", jobRepository)
            .chunk<Map<String, Any>, Map<String, Any>>(properties.chunkSize, transactionManager)
            .reader(sourceReader)
            .writer(targetWriter)
            .build()

    private fun job(name: String, step: Step): Job = JobBuilder(name, jobRepository).start(step).build()

    private fun runner(job: Job, tables: List<Tables>): Runnable {
        return Runnable {
            for (table in tables) {
                val source = table.source
                val target = table.target
                val params = JobParametersBuilder()
                    .addString("key", DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(LocalDateTime.now()))
                    .addString("keyColumnName", table.keyColumnName)
                    .addString("sourceTable", source.schema + "." + source.table)
                    .addString("targetTable", target.schema + "." + target.table)
                    .toJobParameters()

                val execution = jobLauncher.run(job, params)
                if (execution.status == BatchStatus.COMPLETED) {
                    logger.info(
                        "Migration process '{}' from '{}' to '{}' completed successfully (duration: {})",
                        job.name, "${source.schema}.${source.table}", "${target.schema}.${target.table}",
                        Duration.between(execution.endTime?.toLocalTime(), execution.startTime?.toLocalTime())
                    )
                } else {
                    logger.error(
                        "Migration process '{}' from '{}' to '{}' failed with status {}",
                        job.name, "${source.schema}.${source.table}",
                        "${target.schema}.${target.table}", execution.status
                    )
                }
            }
        }
    }
}
