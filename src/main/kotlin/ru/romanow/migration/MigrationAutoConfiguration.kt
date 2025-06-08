package ru.romanow.migration

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.ItemWriter
import org.springframework.batch.item.database.JdbcBatchItemWriter
import org.springframework.batch.item.database.JdbcPagingItemReader
import org.springframework.batch.item.database.Order
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder
import org.springframework.batch.item.database.support.PostgresPagingQueryProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.batch.BatchDataSource
import org.springframework.boot.autoconfigure.batch.BatchTransactionManager
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.jdbc.core.ColumnMapRowMapper
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType
import org.springframework.jdbc.support.JdbcTransactionManager
import org.springframework.transaction.PlatformTransactionManager
import ru.romanow.migration.config.JobBeanRegistrar
import ru.romanow.migration.properties.MigrationProperties
import ru.romanow.migration.writer.DynamicJdbcBatchItemWriter
import javax.sql.DataSource

@AutoConfiguration
@EnableBatchProcessing(dataSourceRef = "batchDataSource", transactionManagerRef = "batchTransactionManager")
@EnableConfigurationProperties(MigrationProperties::class)
class MigrationAutoConfiguration {
    private val logger = LoggerFactory.getLogger(javaClass)

    @PostConstruct
    fun init() {
        logger.info("Migration configuration applied")
    }

    @Bean
    @BatchDataSource
    fun batchDataSource(): DataSource = EmbeddedDatabaseBuilder()
        .setType(EmbeddedDatabaseType.H2)
        .addScript("/org/springframework/batch/core/schema-h2.sql")
        .generateUniqueName(true)
        .build()

    @Bean
    @BatchTransactionManager
    fun batchTransactionManager() = JdbcTransactionManager(batchDataSource())

    @Bean
    @StepScope
    fun sourceReader(
        @Value("#{jobParameters['sourceTable']}") sourceTableName: String?,
        @Value("#{jobParameters['keyColumnName']}") keyColumnName: String?,
        @Qualifier("sourceDataSource") dataSource: DataSource,
        properties: MigrationProperties
    ): JdbcPagingItemReader<Map<String, Any>> {
        val provider = PostgresPagingQueryProvider()
        provider.setSelectClause("SELECT *")
        provider.setFromClause("FROM $sourceTableName")
        provider.sortKeys = mapOf(keyColumnName to Order.ASCENDING)
        return JdbcPagingItemReaderBuilder<Map<String, Any>>()
            .dataSource(dataSource)
            .queryProvider(provider)
            .saveState(false)
            .pageSize(properties.chunkSize)
            .rowMapper(ColumnMapRowMapper())
            .build()
    }

    @Bean
    @StepScope
    fun targetWriter(
        @Value("#{jobParameters['targetTable']}") targetTableName: String?,
        @Qualifier("targetDataSource") dataSource: DataSource
    ): JdbcBatchItemWriter<Map<String, Any>> {
        return DynamicJdbcBatchItemWriter(targetTableName, dataSource)
    }

    @Bean
    fun jobBeanRegistrar(
        properties: MigrationProperties,
        sourceReader: ItemReader<Map<String, Any>>,
        targetWriter: ItemWriter<Map<String, Any>>,
        jobLauncher: JobLauncher,
        jobRepository: JobRepository,
        transactionManager: PlatformTransactionManager
    ): JobBeanRegistrar {
        return JobBeanRegistrar(properties, jobRepository, transactionManager, sourceReader, targetWriter, jobLauncher)
    }
}
