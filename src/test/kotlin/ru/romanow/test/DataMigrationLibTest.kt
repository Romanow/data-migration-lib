package ru.romanow.test

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Test
import org.springframework.batch.core.JobParameter
import org.springframework.batch.item.ItemProcessor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.boot.ApplicationRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.jdbc.core.BeanPropertyRowMapper
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.ActiveProfiles
import ru.romanow.migration.config.BatchJobRunner
import ru.romanow.migration.constansts.FieldMap
import ru.romanow.migration.constansts.SOURCE_DATASOURCE_NAME
import ru.romanow.migration.constansts.TARGET_DATASOURCE_NAME
import ru.romanow.migration.processors.JobContextAware
import ru.romanow.migration.processors.ProcessorFactory
import ru.romanow.migration.properties.FieldOperation
import ru.romanow.migration.properties.MigrationProperties
import ru.romanow.test.config.DatabaseTestConfiguration
import ru.romanow.test.entities.Users
import java.util.*
import java.util.zip.CRC32
import javax.sql.DataSource

@ActiveProfiles("test")
@SpringBootTest
@Import(DatabaseTestConfiguration::class)
internal class DataMigrationLibTest {

    @Autowired
    @Qualifier(TARGET_DATASOURCE_NAME)
    private lateinit var targetDataSource: DataSource

    @Autowired
    @Qualifier(SOURCE_DATASOURCE_NAME)
    private lateinit var sourceDataSource: DataSource

    @Autowired
    private lateinit var properties: MigrationProperties

    @Test
    fun test() {
        val target = properties.tables.first().target
        val targetTableName = "${target.schema}.${target.table}"

        val sourceJdbcTemplate = JdbcTemplate(sourceDataSource)
        val countries = sourceJdbcTemplate.queryForList("SELECT * FROM countries", String::class.java)
        val names = sourceJdbcTemplate.queryForList("SELECT * FROM names", String::class.java)

        val targetJdbcTemplate = JdbcTemplate(targetDataSource)
        val users = targetJdbcTemplate.query("SELECT * FROM $targetTableName", BeanPropertyRowMapper(Users::class.java))

        assertThat(users).hasSize(20000)
        for (user in users) {
            val sa = SoftAssertions()
            sa.assertThat(user.uid).isNotNull
            sa.assertThat(user.solveId).isEqualTo(SOLVE_ID)
            sa.assertThat(names).contains(user.name)
            sa.assertThat(user.age).isBetween(18, 99)
            sa.assertThat(countries).contains(user.location)
            sa.assertThat(user.createdDate).isNotNull
            sa.assertThat(user.checksum).isNotNull
        }
    }

    @SpringBootApplication
    internal class TestApplication {

        @Bean
        fun runner(runners: List<BatchJobRunner>) = ApplicationRunner {
            runners.forEach { it.run(mapOf("solveId" to SOLVE_ID)) }
        }

        @Bean
        fun checksumProcessor(): ProcessorFactory {
            return object : ProcessorFactory {
                override fun create(
                    field: FieldOperation, jobContextAware: JobContextAware?
                ): ItemProcessor<FieldMap, FieldMap> {
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

    companion object {
        private val SOLVE_ID = UUID.fromString("23df0e37-c989-4277-80b8-f53c38ff99e9")
    }
}
