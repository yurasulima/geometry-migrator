package geometry.migration

import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonParser

fun String.toJsonObject(): JsonObject =
    JsonParser.parseString(trimIndent()).asJsonObject

fun JsonObject.obj(key: String): JsonObject =
    getAsJsonObject(key)
        ?: error("Expected object at key '$key', got: ${get(key)}")

fun JsonObject.arr(key: String) =
    getAsJsonArray(key)
        ?: error("Expected array at key '$key', got: ${get(key)}")

fun JsonObject.str(key: String): String =
    get(key)?.asString
        ?: error("Expected string at key '$key'")

fun JsonObject.int(key: String): Int =
    get(key)?.asInt
        ?: error("Expected int at key '$key'")

fun JsonObject.float(key: String): Float =
    get(key)?.asFloat
        ?: error("Expected float at key '$key'")

fun JsonObject.bool(key: String): Boolean =
    get(key)?.asBoolean
        ?: error("Expected bool at key '$key'")

fun JsonObject.has(key: String): Boolean = has(key)

/** Returns the first element of the `minecraft:geometry` array as a JsonObject. */
fun JsonObject.firstGeometry(): JsonObject =
    arr("minecraft:geometry")[0].asJsonObject

/** Returns the first bone of the first geometry entry. */
fun JsonObject.firstBone(): JsonObject =
    firstGeometry().arr("bones")[0].asJsonObject

/** Returns the bone at the given index from the first geometry entry. */
fun JsonObject.bone(index: Int): JsonObject =
    firstGeometry().arr("bones")[index].asJsonObject

/** Returns the first cube of the first bone in the first geometry entry. */
fun JsonObject.firstCube(): JsonObject =
    firstBone().arr("cubes")[0].asJsonObject

/** Returns the selected cube from the selected bone in the first geometry entry. */
fun JsonObject.cubeOf(boneIndex: Int, cubeIndex: Int = 0): JsonObject =
    bone(boneIndex).arr("cubes")[cubeIndex].asJsonObject

fun JsonElement.floatAt(index: Int) = asJsonArray[index].asFloat
