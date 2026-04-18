package geometry.migration.steps

import com.google.gson.JsonObject
import geometry.migration.GeometryMigrationStep
import geometry.version.GeometryVersion

/**
 * Migration from 1.19.30 to 1.21.0.
 *
 * Changes:
 *  1. Per-face UV objects gained the optional `uv_rotation` integer field.
 *     The value is a clockwise rotation angle in degrees, in 90-degree increments.
 *     If the field is absent, no rotation is applied.
 *
 *  2. Geometry entries gained the optional `item_display_transforms` block for
 *     item render transforms in contexts such as `gui`, `firstperson_righthand`,
 *     `firstperson_lefthand`, `thirdperson_righthand`, `thirdperson_lefthand`,
 *     `ground`, `fixed`, and `head`.
 *     Each context may define `translation`, `rotation`, `scale`,
 *     `rotation_pivot`, `scale_pivot`, and `fit_to_frame` for `gui`.
 *
 * Both additions are optional, so existing data does not need to change.
 * This migration only updates `format_version`.
 *
 * Example `item_display_transforms` structure:
 *
 * "item_display_transforms": {
 *   "gui": {
 *     "translation": [0, 0, 0],
 *     "rotation": [30, 225, 0],
 *     "scale": [0.625, 0.625, 0.625],
 *     "fit_to_frame": true
 *   },
 *   "ground": {
 *     "translation": [0, 3, 0],
 *     "scale": [0.25, 0.25, 0.25]
 *   }
 * }
 */
class Migrate_1_19_30_To_1_21_0 : GeometryMigrationStep {
    override val from = GeometryVersion.V1_19_30
    override val to   = GeometryVersion.V1_21_0

    override fun migrate(json: JsonObject): JsonObject =
        copyWithNewVersion(json, to.versionString)
}
