package geometry.migration.steps

import com.google.gson.*
import geometry.migration.GeometryMigrationStep
import geometry.version.GeometryVersion

/**
 * Migration from 1.12.0 to 1.14.0.
 *
 * Changes:
 *  1. Cubes convert simple `"uv": [u, v]` values into per-face UV objects
 *     using standard Minecraft box-UV coordinates.
 *  2. In 1.12, cube pivot values were flipped on the Y axis because of an engine bug.
 *     That was fixed in 1.14. Since it cannot be corrected safely in all cases
 *  3. Locators convert from legacy `[x, y, z]` arrays to `{ "offset": [x, y, z] }` objects.
 */
class Migrate_1_12_0_To_1_14_0 : GeometryMigrationStep {
    override val from = GeometryVersion.V1_12_0
    override val to   = GeometryVersion.V1_14_0

    override fun migrate(json: JsonObject): JsonObject {
        val result = copyWithNewVersion(json, to.versionString)

        result.get("minecraft:geometry")?.asJsonArray?.forEach { entryEl ->
            val entry = entryEl.asJsonObject
            entry.get("bones")?.asJsonArray?.forEach { boneEl ->
                val bone = boneEl.asJsonObject
                migrateCubes(bone)
                migrateLocators(bone)
            }
        }

        return result
    }

    /**
     * Converts a simple `uv` array into a per-face object.
     *
     * Standard Minecraft box UV layout:
     *
     *         [sz]  [sx]  [sz]  [sx]
     *  [sz]   up          down
     *  [sy]   east  north west  south
     *
     * Which means:
     *   east:  (u,           v+sz),  size=(sz, sy)
     *   north: (u+sz,        v+sz),  size=(sx, sy)
     *   west:  (u+sz+sx,     v+sz),  size=(sz, sy)
     *   south: (u+sz+sx+sz,  v+sz),  size=(sx, sy)
     *   up:    (u+sz,        v),     size=(sx, sz)
     *   down:  (u+sz+sx,     v),     size=(sx, sz)
     */
    private fun migrateCubes(bone: JsonObject) {
        val cubes = bone.get("cubes")?.asJsonArray ?: return

        cubes.forEach { cubeEl ->
            val cube = cubeEl.asJsonObject
            val uvEl = cube.get("uv") ?: return@forEach
            if (!uvEl.isJsonArray) return@forEach

            val uvArr = uvEl.asJsonArray
            val u = uvArr[0].asFloat
            val v = uvArr[1].asFloat

            val size = cube.get("size")?.asJsonArray
            val sx = size?.get(0)?.asFloat ?: 0f
            val sy = size?.get(1)?.asFloat ?: 0f
            val sz = size?.get(2)?.asFloat ?: 0f

            val perFace = JsonObject().apply {
                add("north", face(u + sz,               v + sz, sx, sy))
                add("south", face(u + sz + sx + sz,     v + sz, sx, sy))
                add("east",  face(u,                    v + sz, sz, sy))
                add("west",  face(u + sz + sx,          v + sz, sz, sy))
                add("up",    face(u + sz,               v,      sx, sz))
                add("down",  face(u + sz + sx,          v,      sx, sz))
            }

            cube.remove("uv")
            cube.add("uv", perFace)


        }
    }

    private fun face(u: Float, v: Float, w: Float, h: Float): JsonObject =
        JsonObject().apply {
            add("uv", JsonArray().apply { add(u); add(v) })
            add("uv_size", JsonArray().apply { add(w); add(h) })
        }

    /**
     * Converts an array-form locator into an object with `offset`.
     */
    private fun migrateLocators(bone: JsonObject) {
        val locators = bone.get("locators")?.asJsonObject ?: return
        val newLocators = JsonObject()

        locators.entrySet().forEach { (id, locEl) ->
            if (locEl.isJsonArray) {
                newLocators.add(id, JsonObject().apply { add("offset", locEl) })
            } else {
                newLocators.add(id, locEl)
            }
        }

        bone.remove("locators")
        bone.add("locators", newLocators)
    }
}
