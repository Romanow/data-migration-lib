package ru.romanow.migration.processors

import org.springframework.batch.core.StepExecution
import org.springframework.batch.core.annotation.BeforeStep

/**
 * Т.к. `JobParameters` является StepScope, то и при ручном создании bean они не видны из beanFactory:
 * `beanFactory.getBean("jobParameters")` кидает `NoSuchBeanDefinitionException`
 * `JobContextAware` передается в `StepBuilder` и служит holder'ом для `JobParameters`.
 */
class JobContextAware {
    var jobParameters = emptyMap<String, Any>()

    @BeforeStep
    fun beforeStep(stepExecution: StepExecution) {
        this.jobParameters = stepExecution.jobParameters.parameters.mapValues { it.value.value }
    }
}
