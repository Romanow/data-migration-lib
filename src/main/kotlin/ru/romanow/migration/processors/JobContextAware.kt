package ru.romanow.migration.processors

interface JobContextAware {
    fun notify(jobParameters: Map<String, Any>) {}
}
