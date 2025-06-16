package ru.romanow.test

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.batch.item.ItemProcessor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import ru.romanow.migration.config.BatchJobRunner
import ru.romanow.migration.constansts.FieldMap
import ru.romanow.migration.constansts.TARGET_DATASOURCE_NAME
import ru.romanow.migration.processors.ProcessorFactory
import ru.romanow.migration.properties.FieldOperation
import ru.romanow.migration.properties.MigrationProperties
import ru.romanow.test.config.DatabaseTestConfiguration
import java.util.zip.CRC32
import javax.sql.DataSource

@ActiveProfiles("test")
@SpringBootTest
@Import(DatabaseTestConfiguration::class)
internal class DataMigrationLibTest {

    @Autowired
    @Qualifier(TARGET_DATASOURCE_NAME)
    private lateinit var dataSource: DataSource

    @Autowired
    private lateinit var properties: MigrationProperties

    @Test
    fun test() {
        val jdbcTemplate = JdbcTemplate(dataSource)
        val target = properties.tables.first().target
        val targetTableName = "${target.schema}.${target.table}"
        val count = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM $targetTableName", Long::class.java)!!
        assertThat(count).isEqualTo(20000)
    }

    @SpringBootApplication
    internal class TestApplication {

        @Bean
        fun runner(runners: List<BatchJobRunner>) = ApplicationRunner {
            runners.forEach { it.run() }
        }

        @Bean
        fun checksumProcessor(): ProcessorFactory {
            return object : ProcessorFactory {
                override fun create(field: FieldOperation): ItemProcessor<FieldMap, FieldMap> {
                    val objectMapper = jacksonObjectMapper().findAndRegisterModules()
                    return ItemProcessor {
                        val data = objectMapper.writeValueAsString(it)
                        val checksum = CRC32().apply { update(data.toByteArray()) }.value
                        it[field.target?.name!!] = checksum
                        return@ItemProcessor it
                    }
                }
            }
        }
    }
}
