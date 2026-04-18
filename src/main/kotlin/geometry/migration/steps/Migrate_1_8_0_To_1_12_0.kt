package geometry.migration.steps

import com.google.gson.*
import geometry.migration.GeometryMigrationStep
import geometry.version.GeometryVersion

/**
 * Migration from 1.8.0 to 1.12.0.
 *
 * Changes:
 *  1. Top-level structure:
 *       `"geometry.<name>": { ... }`
 *     becomes
 *       `"minecraft:geometry": [ { "description": { "identifier": "geometry.<name>", ... }, ... } ]`
 *  2. Texture size fields move into `description` and are renamed:
 *       `texturewidth`  -> `texture_width`
 *       `textureheight` -> `texture_height`
 *  3. `visible_bounds_*` fields move from the model root into `description`.
 *  4. Bone fields `reset`, `neverRender`, and `bind_pose_rotation` are removed.
 *  5. Cubes support per-face UV objects, while plain `[u, v]` arrays are still accepted.
 *  6. Locators support the object form `{ offset, rotation, ignore_inherited_scale }`.
 */
class Migrate_1_8_0_To_1_12_0 : GeometryMigrationStep {
    override val from = GeometryVersion.V1_8_0
    override val to   = GeometryVersion.V1_12_0

    override fun migrate(json: JsonObject): JsonObject {
        val result = JsonObject()
        result.addProperty("format_version", to.versionString)
        json.get("debug")?.let { result.add("debug", it) }

        val geometryArray = JsonArray()

        json.entrySet()
            .filter { it.key.startsWith("geometry.") }
            .forEach { (key, modelEl) ->
                val model = modelEl.asJsonObject
                val entry = JsonObject()

                val description = JsonObject()
                description.addProperty("identifier", key)
                model.get("visible_bounds_width")?.let  { description.add("visible_bounds_width", it) }
                model.get("visible_bounds_height")?.let { description.add("visible_bounds_height", it) }
                model.get("visible_bounds_offset")?.let { description.add("visible_bounds_offset", it) }
                model.get("texturewidth")?.let  { description.addProperty("texture_width", it.asInt) }
                model.get("textureheight")?.let { description.addProperty("texture_height", it.asInt) }
                entry.add("description", description)

                model.get("cape")?.let { entry.add("cape", it) }

                model.get("bones")?.asJsonArray?.let { bonesArr ->
                    val newBones = JsonArray()
                    bonesArr.forEach { boneEl ->
                        newBones.add(migrateBone(boneEl.asJsonObject))
                    }
                    entry.add("bones", newBones)
                }

                geometryArray.add(entry)
            }

        result.add("minecraft:geometry", geometryArray)
        return result
    }

    private fun migrateBone(bone: JsonObject): JsonObject {
        val result = JsonObject()
        val removed = setOf("reset", "neverRender", "bind_pose_rotation")
        bone.entrySet()
            .filter { it.key !in removed }
            .forEach { (k, v) -> result.add(k, v) }
        return result
    }
}
