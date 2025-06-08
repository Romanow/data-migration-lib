[![CI](https://github.com/Romanow/data-migration-lib/actions/workflows/build.yml/badge.svg?branch=master)](https://github.com/Romanow/data-migration-lib/actions/workflows/build.yml)
[![pre-commit](https://img.shields.io/badge/pre--commit-enabled-brightgreen?logo=pre-commit)](https://github.com/pre-commit/pre-commit)
[![Release](https://img.shields.io/github/v/release/Romanow/data-migration-lib?logo=github&sort=semver)](https://github.com/Romanow/data-migration-lib/releases/latest)
[![License](https://img.shields.io/github/license/Romanow/data-migration-lib)](https://github.com/Romanow/data-migration-lib/blob/master/LICENSE)

# Batch process for data migration

1. Для каждой таблицы создавать свой `@Bean` migration, состоящий из read -> process -> write.
2. В каждом migration bean реализовать метод `apply`, который будет выполнять миграцию по заданной стратегии.
3. Стратегии модификации данных:
    1. 1 к 1: поднимаем в `Map<String, Object>`, сохраняем в `Map<String, Object>`.
    2. Маппинг полей: если поле указано, то выполняется изменение, иначе 1 к 1.
    3. Изменение типа данных.
    4. Дополнительные поля: использовать SpEL для задания значений (`#{jobParameters['operationUid']`,
       `${random.uuid}` и т.п.).

Mapping:

* Если задано только `source` и `target`, то просто меняем имена в `Map<String, Object>`.
* Если задан `default-value`, то в случае если в `source[fieldName] == null`, то `source[fieldName] = defaultValue` (
  можно использовать SpEL).
* Если задан `target-type`, то ищется нужный `Converter` и применяется к этому типу данных.

Конфигурация:

```yaml
migration:
  chunk-size: ${CHUNK_SIZE:5000}
  tables:
    - key-column-name: uid
      source:
        schema: public
        table: users
      target:
        schema: public
        table: users
      mapping:
        - source-name: solve_id
          source-type: String
          target-name: process_uid
          target-type: java.util.UUID
          default-value: ${random.uuid}
      additional-fields:
        - name: solve_uid
          type: java.util.UUID
          value: ${random.uuid}
```

Считываем `migration.jobs`, для каждой job создаем
