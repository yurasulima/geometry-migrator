package geometry.migration.steps

import com.google.gson.JsonObject
import geometry.migration.GeometryMigrationStep
import geometry.version.GeometryVersion

/**
 * Migration from 1.14.0 to 1.16.0.
 *
 * Changes:
 *  1. Bones gained the optional `binding` field, a Molang string used by
 *     items and attachables to bind the bone into the parent skeleton hierarchy.
 *     Example: `q.item_slot_to_bone_name(context.item_slot)`.
 *
 * There are no structural schema changes, so this migration only updates `format_version`.
 */
class Migrate_1_14_0_To_1_16_0 : GeometryMigrationStep {
    override val from = GeometryVersion.V1_14_0
    override val to   = GeometryVersion.V1_16_0

    override fun migrate(json: JsonObject): JsonObject =
        copyWithNewVersion(json, to.versionString)
}
