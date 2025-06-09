package ru.romanow.test.config

import com.zaxxer.hikari.HikariDataSource
import org.postgresql.Driver
import org.slf4j.LoggerFactory
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.core.io.ClassPathResource
import org.springframework.core.io.Resource
import org.springframework.jdbc.datasource.init.DataSourceInitializer
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.output.Slf4jLogConsumer
import ru.romanow.migration.MigrationAutoConfiguration.Companion.SOURCE_DATASOURCE_NAME
import ru.romanow.migration.MigrationAutoConfiguration.Companion.TARGET_DATASOURCE_NAME
import javax.sql.DataSource

typealias CustomPostgresContainer = PostgreSQLContainer<*>

@TestConfiguration
class DatabaseTestConfiguration {
    private val logger = LoggerFactory.getLogger(javaClass)

    @Bean
    fun postgres(): PostgreSQLContainer<*> =
        CustomPostgresContainer(POSTGRES_IMAGE)
            .withUsername(USERNAME)
            .withPassword(PASSWORD)
            .withInitScript("scripts/10-create-databases.sql")
            .withLogConsumer(Slf4jLogConsumer(logger))

    @Bean(SOURCE_DATASOURCE_NAME)
    fun sourceDataSource(): HikariDataSource {
        val dataSource = HikariDataSource()
        dataSource.jdbcUrl = "jdbc:postgresql://localhost:${postgres().getMappedPort(POSTGRES_PORT)}/$SOURCE_DATABASE"
        dataSource.username = USERNAME
        dataSource.password = PASSWORD
        dataSource.driverClassName = Driver::class.java.canonicalName
        return dataSource
    }

    @Bean(TARGET_DATASOURCE_NAME)
    fun targetDataSource(): HikariDataSource {
        val dataSource = HikariDataSource()
        dataSource.jdbcUrl = "jdbc:postgresql://localhost:${postgres().getMappedPort(POSTGRES_PORT)}/$TARGET_DATABASE"
        dataSource.username = USERNAME
        dataSource.password = PASSWORD
        dataSource.driverClassName = Driver::class.java.canonicalName
        return dataSource
    }

    @Bean
    fun sourceInitializer() =
        configureInitializer(
            sourceDataSource(),
            ClassPathResource("scripts/20-create-source-tables.sql"),
            ClassPathResource("scripts/30-insert-data-into-source-tables.sql")
        )

    @Bean
    fun targetInitializer() =
        configureInitializer(targetDataSource(), ClassPathResource("scripts/40-create-target-tables.sql"))

    private fun configureInitializer(dataSource: DataSource, vararg scripts: Resource): DataSourceInitializer {
        val initializer = DataSourceInitializer()
        initializer.setDataSource(dataSource)
        initializer.setDatabasePopulator(ResourceDatabasePopulator(*scripts))
        return initializer
    }

    companion object {
        private const val POSTGRES_IMAGE = "postgres:15"
        private const val POSTGRES_PORT = 5432
        private const val SOURCE_DATABASE = "source"
        private const val TARGET_DATABASE = "target"
        private const val USERNAME = "program"
        private const val PASSWORD = "test"
    }
}
