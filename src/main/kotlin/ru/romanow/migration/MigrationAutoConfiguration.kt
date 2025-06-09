package ru.romanow.migration

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.core.launch.JobLauncher
import org.springframework.batch.core.repository.JobRepository
import org.springframework.batch.item.ItemProcessor
import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.ItemWriter
import org.springframework.batch.item.database.JdbcBatchItemWriter
import org.springframework.batch.item.database.JdbcPagingItemReader
import org.springframework.batch.item.database.Order
import org.springframework.batch.item.database.builder.JdbcPagingItemReaderBuilder
import org.springframework.batch.item.database.support.PostgresPagingQueryProvider
import org.springframework.batch.item.support.PassThroughItemProcessor
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.batch.BatchDataSource
import org.springframework.boot.autoconfigure.batch.BatchTransactionManager
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.ConversionService
import org.springframework.core.convert.support.DefaultConversionService
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions
import org.springframework.jdbc.core.ColumnMapRowMapper
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType
import org.springframework.jdbc.support.JdbcTransactionManager
import org.springframework.stereotype.Component
import org.springframework.transaction.PlatformTransactionManager
import ru.romanow.migration.config.MigrationJobRegistrar
import ru.romanow.migration.constansts.*
import ru.romanow.migration.processors.AdditionalFieldProcessorFactory
import ru.romanow.migration.processors.ModifyFieldProcessorFactory
import ru.romanow.migration.processors.ProcessorFactory
import ru.romanow.migration.processors.RemoveFieldProcessorFactory
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

    @StepScope
    @Bean(READ_STAGE_BEAN_NAME)
    @ConditionalOnMissingBean(name = [READ_STAGE_BEAN_NAME])
    fun sourceReader(
        @Value("#{jobParameters['sourceTable']}") sourceTableName: String?,
        @Value("#{jobParameters['keyColumnName']}") keyColumnName: String?,
        @Qualifier(SOURCE_DATASOURCE_NAME) dataSource: DataSource,
        properties: MigrationProperties
    ): JdbcPagingItemReader<MutableMap<String, Any?>> {
        val provider = PostgresPagingQueryProvider()
        provider.setSelectClause("SELECT *")
        provider.setFromClause("FROM $sourceTableName")
        provider.sortKeys = mapOf(keyColumnName to Order.ASCENDING)
        return JdbcPagingItemReaderBuilder<MutableMap<String, Any?>>()
            .dataSource(dataSource)
            .queryProvider(provider)
            .saveState(false)
            .pageSize(properties.chunkSize)
            .rowMapper(ColumnMapRowMapper())
            .build()
    }

    @Bean(PROCESS_STAGE_BEAN_NAME)
    @ConditionalOnMissingBean(name = [PROCESS_STAGE_BEAN_NAME])
    fun itemProcessor(): ItemProcessor<MutableMap<String, Any?>, MutableMap<String, Any?>> {
        return PassThroughItemProcessor()
    }

    @StepScope
    @Bean(WRITE_STAGE_BEAN_NAME)
    @ConditionalOnMissingBean(name = [WRITE_STAGE_BEAN_NAME])
    fun targetWriter(
        @Value("#{jobParameters['targetTable']}") targetTableName: String?,
        @Qualifier(TARGET_DATASOURCE_NAME) dataSource: DataSource
    ): JdbcBatchItemWriter<MutableMap<String, Any?>> {
        return DynamicJdbcBatchItemWriter(targetTableName, dataSource)
    }

    @Bean(CONVERTOR_SERVICE_BEAN_NAME)
    @ConditionalOnMissingBean(name = [CONVERTOR_SERVICE_BEAN_NAME])
    fun convertionService(): ConversionService {
        val conversionService = DefaultConversionService()
        JdbcCustomConversions().registerConvertersIn(conversionService)
        return conversionService
    }

    @Bean(MODIFY_FIELD_PROCESSOR_BEAN_NAME)
    fun modifyFieldsProcessorFactory() = ModifyFieldProcessorFactory(convertionService())

    @Bean(ADDITIONAL_FIELD_PROCESSOR_BEAN_NAME)
    fun additionalFieldProcessorFactory() = AdditionalFieldProcessorFactory()

    @Bean(REMOVE_FIELD_PROCESSOR_BEAN_NAME)
    fun removeFieldProcessorFactory() = RemoveFieldProcessorFactory()

    @Bean
    @ConditionalOnMissingBean
    fun jobBeanRegistrar(
        properties: MigrationProperties,
        @Qualifier(READ_STAGE_BEAN_NAME) reader: ItemReader<MutableMap<String, Any?>>,
        @Qualifier(PROCESS_STAGE_BEAN_NAME) processor: ItemProcessor<MutableMap<String, Any?>, MutableMap<String, Any?>>,
        @Qualifier(WRITE_STAGE_BEAN_NAME) writer: ItemWriter<MutableMap<String, Any?>>,
        processors: Map<String, ProcessorFactory>,
        jobLauncher: JobLauncher,
        jobRepository: JobRepository,
        @BatchTransactionManager transactionManager: PlatformTransactionManager
    ) = MigrationJobRegistrar(
        properties, reader, processor, writer, processors, jobRepository, transactionManager, jobLauncher
    )
}
