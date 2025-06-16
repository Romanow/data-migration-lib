package ru.romanow.migration.processors

import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.annotation.BeforeStep

/**
 * Т.к. `JobParameters` является StepScope, то ручном создании bean они не видны из beanFactory:
 * `beanFactory.getBean("jobParameters")` кидает `NoSuchBeanDefinitionException`
 *
 * `JobContextListener` передается в `StepBuilder` и через механизм listener-notify уведомляет подписчиков,
 * когда получает `jobParameters`.
 */
class JobContextListener {
    private var listeners: MutableList<in JobContextAware> = mutableListOf()

    @BeforeStep
    fun beforeStep(stepExecution: StepExecution) {
        val jobParameters = stepExecution.jobParameters.parameters.mapValues { it.value.value }
        listeners.forEach { (it as JobContextAware).notify(jobParameters) }
    }

    fun addListener(jobContext: JobContextAware) {
        listeners.add(jobContext)
    }
}
