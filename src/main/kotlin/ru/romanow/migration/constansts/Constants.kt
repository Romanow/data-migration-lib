package ru.romanow.migration.constansts

typealias FieldMap = MutableMap<String, Any?>

const val READ_STAGE_BEAN_NAME = "sourceReader"
const val PROCESS_STAGE_BEAN_NAME = "itemProcessor"
const val WRITE_STAGE_BEAN_NAME = "targetWriter"
const val ADDITIONAL_FIELD_PROCESSOR_BEAN_NAME = "additionalFieldsProcessorFactory"
const val REMOVE_FIELD_PROCESSOR_BEAN_NAME = "removeFieldsProcessorFactory"
const val MODIFY_FIELD_PROCESSOR_BEAN_NAME = "modifyFieldsProcessorFactory"
const val CONVERTOR_SERVICE_BEAN_NAME = "defaultConvertionService"
const val SOURCE_DATASOURCE_NAME = "sourceDataSource"
const val TARGET_DATASOURCE_NAME = "targetDataSource"
