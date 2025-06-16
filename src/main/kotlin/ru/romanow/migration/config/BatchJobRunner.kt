package ru.romanow.migration.config

/**
 * `context` используется для того, чтобы передать дополнительные параметры выполнения.
 * В методе `ProcessorFactory::create` класс `JobContextAware` передается вторым необязательным параметром: в нем
 * хранится `jobParameters` контекста выполнения.
 *
 * Для обращения к контексту в SpEL нужно использовать `#jobParameters['solveId']`.
 */
interface BatchJobRunner {
    fun run(context: Map<String, Any> = emptyMap())
}
