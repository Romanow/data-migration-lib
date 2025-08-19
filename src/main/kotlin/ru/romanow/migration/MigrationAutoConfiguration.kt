package ru.romanow.migration

import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.batch.item.ItemProcessor
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
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.core.convert.ConversionService
import org.springframework.core.convert.support.DefaultConversionService
import org.springframework.data.jdbc.core.convert.JdbcCustomConversions
import org.springframework.jdbc.core.ColumnMapRowMapper
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType
import org.springframework.jdbc.support.JdbcTransactionManager
import ru.romanow.migration.constansts.*
import ru.romanow.migration.properties.MigrationProperties
import ru.romanow.migration.utils.StringHelper
import ru.romanow.migration.utils.StringHelper.replaceTemplate
import ru.romanow.migration.writer.DynamicJdbcBatchItemWriter
import javax.sql.DataSource

@AutoConfiguration
@EnableConfigurationProperties(MigrationProperties::class)
@ComponentScan(basePackages = ["ru.romanow.migration.config", "ru.romanow.migration.processors"])
@EnableBatchProcessing(dataSourceRef = "batchDataSource", transactionManagerRef = "batchTransactionManager")
@ConditionalOnProperty(prefix = "migration", name = ["enabled"], havingValue = "true", matchIfMissing = true)
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
        @Value("#{jobParameters}") params: Map<String, Any>,
        @Qualifier(SOURCE_DATASOURCE_NAME) dataSource: DataSource,
        properties: MigrationProperties
    ): JdbcPagingItemReader<FieldMap> {
        val sourceTableName = params["sourceTable"] as String
        val keyColumnName = params["keyColumnName"] as String
        val provider = PostgresPagingQueryProvider()
        provider.setSelectClause("SELECT *")
        provider.setFromClause("FROM $sourceTableName")
        provider.sortKeys = mapOf(keyColumnName to Order.ASCENDING)
        val searchQuery = params["searchQuery"] as String?
        if (searchQuery != null) {
            provider.setWhereClause(replaceTemplate(searchQuery, params))
        }
        return JdbcPagingItemReaderBuilder<FieldMap>()
            .dataSource(dataSource)
            .queryProvider(provider)
            .saveState(false)
            .pageSize(properties.chunkSize)
            .rowMapper(ColumnMapRowMapper())
            .build()
    }

    @Bean(PROCESS_STAGE_BEAN_NAME)
    @ConditionalOnMissingBean(name = [PROCESS_STAGE_BEAN_NAME])
    fun itemProcessor(): ItemProcessor<FieldMap, FieldMap> {
        return PassThroughItemProcessor()
    }

    @StepScope
    @Bean(WRITE_STAGE_BEAN_NAME)
    @ConditionalOnMissingBean(name = [WRITE_STAGE_BEAN_NAME])
    fun targetWriter(
        @Value("#{jobParameters['targetTable']}") targetTableName: String?,
        @Qualifier(TARGET_DATASOURCE_NAME) dataSource: DataSource
    ): JdbcBatchItemWriter<FieldMap> {
        return DynamicJdbcBatchItemWriter(targetTableName, dataSource)
    }

    @Bean(CONVERTOR_SERVICE_BEAN_NAME)
    @ConditionalOnMissingBean(name = [CONVERTOR_SERVICE_BEAN_NAME])
    fun conversionService(): ConversionService {
        val conversionService = DefaultConversionService()
        JdbcCustomConversions().registerConvertersIn(conversionService)
        return conversionService
    }
}
