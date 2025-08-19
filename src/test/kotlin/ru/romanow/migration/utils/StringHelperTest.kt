package ru.romanow.migration.utils

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import ru.romanow.migration.utils.StringHelper.replaceTemplate
import java.time.LocalDate
import java.time.format.DateTimeFormatter.ISO_DATE

class StringHelperTest {

    @Test
    fun test() {
        val date = LocalDate.of(2025, 1, 1)
        val map = mapOf("requiredAge" to 18, "retired" to false, "min" to ISO_DATE.format(date))
        val sql = "WHERE age > #{requiredAge} AND retired = #{retired} AND create_date BETWEEN #{min} AND #{max}"

        assertThat(replaceTemplate(sql, map))
            .isEqualTo("WHERE age > 18 AND retired = false AND create_date BETWEEN '2025-01-01' AND '#{max}'")
    }
}
