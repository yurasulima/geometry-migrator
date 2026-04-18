package geometry.migration

import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import geometry.migration.steps.*
import geometry.version.GeometryVersion

/**
 * Main migration orchestrator.
 *
 * Automatically builds the chain of steps from the file's current version
 * to the requested target version, which defaults to the latest one.
 *
 * Usage:
 * ```kotlin
 * val migrator = GeometryMigrator()
 *
 * val newJson = migrator.migrateJson(oldJsonString)
 * val newObj = migrator.migrate(jsonObject, GeometryVersion.V1_16_0)
 * ```
 */
class GeometryMigrator {

    /** Registered steps in strict order from oldest to newest. */
    private val steps: List<GeometryMigrationStep> = listOf(
        Migrate_1_8_0_To_1_12_0(),
        Migrate_1_12_0_To_1_14_0(),
        Migrate_1_14_0_To_1_16_0(),
        Migrate_1_16_0_To_1_19_30(),
        Migrate_1_19_30_To_1_21_0(),
    )

    private val gson = GsonBuilder().setPrettyPrinting().create()

    /**
     * Migrates a JSON string to [targetVersion].
     * Returns a pretty-printed JSON string.
     */
    fun migrateJson(
        jsonString: String,
        targetVersion: GeometryVersion = GeometryVersion.V1_21_0
    ): String {
        val jsonObject = JsonParser.parseString(jsonString).asJsonObject
        val result = migrate(jsonObject, targetVersion)
        return gson.toJson(result)
    }

    /**
     * Migrates [json] to [targetVersion].
     * Only the steps between the file's current version and the target version
     * are applied, in order.
     *
     * @throws IllegalArgumentException if `format_version` is unknown
     *         or if the requested migration would go backwards.
     */
    fun migrate(
        json: JsonObject,
        targetVersion: GeometryVersion = GeometryVersion.V1_21_0
    ): JsonObject {
        val versionStr = json.get("format_version")?.asString
            ?: throw IllegalArgumentException("Missing \"format_version\" in geometry file.")

        var currentVersion = GeometryVersion.fromString(versionStr)

        require(currentVersion.ordinal <= targetVersion.ordinal) {
            "Cannot migrate backwards: current=$currentVersion, target=$targetVersion"
        }

        if (currentVersion == targetVersion) return json

        var result = json

        steps
            .filter { step ->
                step.from.ordinal >= currentVersion.ordinal &&
                step.to.ordinal   <= targetVersion.ordinal
            }
            .forEach { step ->
                result = step.migrate(result)
                currentVersion = step.to
            }

        return result
    }
}
