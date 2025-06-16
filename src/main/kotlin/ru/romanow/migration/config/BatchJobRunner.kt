package ru.romanow.migration.config

@FunctionalInterface
interface BatchJobRunner {
    fun run()
}
