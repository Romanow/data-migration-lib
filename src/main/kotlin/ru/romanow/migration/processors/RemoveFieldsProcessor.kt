package ru.romanow.migration.processors

import org.springframework.batch.item.ItemProcessor

class RemoveFieldsProcessor(
    private val ignoreFields: List<String>
) : ItemProcessor<MutableMap<String, Any>, MutableMap<String, Any>> {
    override fun process(item: MutableMap<String, Any>): MutableMap<String, Any> {
        ignoreFields.forEach { item.remove(it) }
        return item
    }
}
