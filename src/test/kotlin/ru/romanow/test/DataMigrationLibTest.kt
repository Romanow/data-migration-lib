package ru.romanow.test

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Test
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
import ru.romanow.migration.processors.ProcessorFactory
import ru.romanow.migration.properties.FieldOperation
import ru.romanow.migration.properties.OperationType
import ru.romanow.test.config.DatabaseTestConfiguration
import ru.romanow.test.entities.Operations
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

    @Test
    fun test() {
        val sourceJdbcTemplate = JdbcTemplate(sourceDataSource)
        val countries = sourceJdbcTemplate.queryForList("SELECT * FROM countries", String::class.java)
        val names = sourceJdbcTemplate.queryForList("SELECT * FROM names", String::class.java)

        val targetJdbcTemplate = JdbcTemplate(targetDataSource)
        val users = targetJdbcTemplate.query("SELECT * FROM users", BeanPropertyRowMapper(Users::class.java))

        assertThat(users).hasSize(20000)
        for (user in users) {
            val sa = SoftAssertions()
            sa.assertThat(user.uid).isNotNull
            sa.assertThat(user.solveId).isEqualTo(USER_SOLVE_ID)
            sa.assertThat(names).contains(user.name)
            sa.assertThat(user.age).isBetween(18, 99)
            sa.assertThat(countries).contains(user.location)
            sa.assertThat(user.createdDate).isNotNull
            sa.assertThat(user.checksum).isNotNull
        }

        val operations =
            targetJdbcTemplate.query("SELECT * FROM operations", BeanPropertyRowMapper(Operations::class.java))

        assertThat(operations).hasSize(20000)
        for (operation in operations) {
            val sa = SoftAssertions()
            sa.assertThat(operation.processId).isNotNull
            sa.assertThat(operation.solveId).isEqualTo(OPERATION_SOLVE_ID)
            sa.assertThat(OperationType.entries).contains(operation.type)
            sa.assertThat(operation.startedAt).isNotNull
            sa.assertThat(names).contains(operation.startedBy)
        }
    }

    @SpringBootApplication
    internal class TestApplication {

        @Bean
        fun runner(runners: Map<String, BatchJobRunner>) = ApplicationRunner {
            runners["users-migration"]?.run(mapOf("solveId" to USER_SOLVE_ID))
            runners["operations-migration"]?.run(mapOf("solveId" to OPERATION_SOLVE_ID))
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

    companion object {
        private val USER_SOLVE_ID = UUID.fromString("23df0e37-c989-4277-80b8-f53c38ff99e9")
        private val OPERATION_SOLVE_ID = UUID.fromString("db0fa6d2-c38b-4c2a-9785-aba9180b40d5")
    }
}
