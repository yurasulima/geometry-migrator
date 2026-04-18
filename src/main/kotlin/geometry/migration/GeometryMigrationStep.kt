package geometry.migration

import com.google.gson.JsonObject
import geometry.version.GeometryVersion

interface GeometryMigrationStep {
    val from: GeometryVersion
    val to: GeometryVersion

    /** Accepts a valid JsonObject of the current version and returns a JsonObject of the next version. */
    fun migrate(json: JsonObject): JsonObject
}
