package ru.romanow.migration.config

/**
 * `context` используется для того, чтобы передать дополнительные параметры выполнения.
 * Для получения параметров выполнения в `ProcessorFactory` (которые не имеют step scope) используется слушатель:
 * ```
 * @BeforeStep
 * fun beforeStep(stepExecution: StepExecution) {}
 * ```
 * где из контекста `stepExecution` выполнения берутся `jobParameters` и вызывается метод `notify` из для всех
 * подписчиков, реализующих `JobContextAware`.
 *
 * Для обращения к контексту в SpEL нужно использовать `#jobParameters['solveId']`.
 */
interface BatchJobRunner {
    fun run(context: Map<String, Any> = emptyMap())
}
