package ru.romanow.migration.utils

import java.util.regex.Matcher

object StringHelper {
    fun replaceTemplate(template: String, values: Map<String, Any>): String {
        return SEARCH_PATTERN.replace(template) {
            val key = it.groupValues[1]
            val value = values[key] ?: it.value
            quoteReplacement(value)
        }
    }

    private fun quoteReplacement(value: Any) = when (value) {
        is Number -> value.toString()
        is Boolean -> value.toString()
        else -> "'${Matcher.quoteReplacement(value.toString())}'"
    }

    private val SEARCH_PATTERN = "#\\{(\\w+)}".toRegex()
}
