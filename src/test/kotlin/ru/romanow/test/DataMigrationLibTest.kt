package ru.romanow.test

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import ru.romanow.migration.properties.MigrationProperties
import ru.romanow.test.config.DatabaseTestConfiguration
import javax.sql.DataSource

@ActiveProfiles("test")
@SpringBootTest
@Import(DatabaseTestConfiguration::class)
internal class DataMigrationLibTest {

    @Autowired
    @Qualifier("targetDataSource")
    private lateinit var dataSource: DataSource

    @Autowired
    private lateinit var properties: MigrationProperties

    @Test
    fun test() {
        val jdbcTemplate = JdbcTemplate(dataSource)
        val target = properties.jobs.first().tables.first().target
        val targetTableName = "${target.schema}.${target.table}"
        val count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM $targetTableName", Long::class.java)!!
        assertThat(count).isEqualTo(20000)
    }

    @SpringBootApplication
    internal class TestApplication {

        @Bean
        fun runner(runners: List<Runnable>) = ApplicationRunner {
            runners.forEach { it.run() }
        }
    }
}
