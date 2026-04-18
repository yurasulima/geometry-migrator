package geometry.migration.steps

import com.google.gson.JsonObject
import geometry.migration.GeometryMigrationStep
import geometry.version.GeometryVersion

/**
 * Migration from 1.16.0 to 1.19.30.
 *
 * Changes between these versions only affect internal engine behavior
 * such as rendering and animation processing. The JSON schema is unchanged.
 *
 * This migration only updates `format_version`.
 */
class Migrate_1_16_0_To_1_19_30 : GeometryMigrationStep {
    override val from = GeometryVersion.V1_16_0
    override val to   = GeometryVersion.V1_19_30

    override fun migrate(json: JsonObject): JsonObject =
        copyWithNewVersion(json, to.versionString)
}
