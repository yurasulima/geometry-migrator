# Geometry Migrator

**Modern Kotlin library for migrating Minecraft Bedrock Edition geometry JSON across format versions.**

Convert legacy `.geo.json` files into newer Bedrock geometry formats with a simple API, predictable migration steps, and test-covered behavior.

![Maven Central Version](https://img.shields.io/maven-central/v/io.github.yurasulima/geometry-migrator?style=for-the-badge)
![Kotlin](https://img.shields.io/badge/kotlin-1.9.23-1f6feb?style=for-the-badge)
![Java](https://img.shields.io/badge/java-17+-ea580c?style=for-the-badge)
![License](https://img.shields.io/badge/license-MIT-111827?style=for-the-badge)

## Overview

`geometry-migrator` helps you upgrade Minecraft Bedrock Edition geometry files from older schema versions to newer ones without hand-editing JSON.

It is built for tools, pipelines, converters, pack editors, and backend services that need to:

- migrate old geometry definitions to supported Bedrock formats
- preserve structure and important model metadata
- normalize changes between version-specific schema differences
- use a small, dependency-light library in JVM projects

## Highlights

- Supports migrations from `1.8.0` through `1.21.0`
- Clean Kotlin API with both `String` and `JsonObject` entry points
- Sequential migration chain between intermediate versions
- Pretty-printed JSON output for string-based migration
- Test suite covering migration behavior and edge cases
- Published as a reusable JVM library

## Supported Migration Path

The library currently includes these migration steps:

| From | To |
| --- | --- |
| `1.8.0` | `1.12.0` |
| `1.12.0` | `1.14.0` |
| `1.14.0` | `1.16.0` |
| `1.16.0` | `1.19.30` |
| `1.19.30` | `1.21.0` |

Default target version: `1.21.0`

Current library version: see the Maven Central badge above.

## Installation

### Gradle Kotlin DSL

```kotlin
dependencies {
    implementation("io.github.yurasulima:geometry-migrator:1.0.6")
}
```

### Gradle Groovy DSL

```groovy
dependencies {
    implementation 'io.github.yurasulima:geometry-migrator:1.0.6'
}
```

### Maven

```xml
<dependency>
  <groupId>io.github.yurasulima</groupId>
  <artifactId>geometry-migrator</artifactId>
  <version>1.0.6</version>
</dependency>
```

## Quick Start

### Migrate a JSON string to the latest supported version

```kotlin
import geometry.migration.GeometryMigrator

val migrator = GeometryMigrator()
val migratedJson = migrator.migrateJson(oldJson)
```

### Migrate to a specific target version

```kotlin
import geometry.migration.GeometryMigrator
import geometry.version.GeometryVersion

val migrator = GeometryMigrator()
val migratedJson = migrator.migrateJson(oldJson, GeometryVersion.V1_16_0)
```

### Work with `JsonObject`

```kotlin
import com.google.gson.JsonParser
import geometry.migration.GeometryMigrator
import geometry.version.GeometryVersion

val migrator = GeometryMigrator()
val source = JsonParser.parseString(oldJson).asJsonObject
val migrated = migrator.migrate(source, GeometryVersion.V1_21_0)
```

## What Gets Migrated

Depending on the source and target versions, the migrator handles schema changes such as:

- top-level geometry restructuring
- `format_version` upgrades
- texture field renaming
- movement of `description` fields
- removal of obsolete bone properties
- conversion of simple UV arrays to per-face UV objects
- locator shape normalization

Some version bumps only update `format_version` when the underlying geometry schema remains compatible.

## Example Use Cases

- Bedrock resource pack migration tools
- geometry preprocessors in content pipelines
- internal editor utilities
- automated asset cleanup during CI
- importing older community models into newer projects

## API

Main entry point:

```kotlin
class GeometryMigrator {
    fun migrateJson(
        jsonString: String,
        targetVersion: GeometryVersion = GeometryVersion.V1_21_0
    ): String

    fun migrate(
        json: JsonObject,
        targetVersion: GeometryVersion = GeometryVersion.V1_21_0
    ): JsonObject
}
```

Version enum:

```kotlin
enum class GeometryVersion {
    V1_8_0,
    V1_12_0,
    V1_14_0,
    V1_16_0,
    V1_19_30,
    V1_21_0
}
```

## Development

Run tests:

```bash
./gradlew test
```

Build the library:

```bash
./gradlew build
```

## License

Released under the [MIT License](./LICENSE).

## Author

Created by [Yurii Sulyma](https://github.com/yurasulima).
