package geometry.migration.steps

import com.google.gson.JsonObject
import com.google.gson.JsonParser

/**
 * Creates a deep copy of a JsonObject by reparsing its string form.
 * Replaces `format_version` with the provided version.
 */
fun copyWithNewVersion(json: JsonObject, version: String): JsonObject {
    val copy = JsonParser.parseString(json.toString()).asJsonObject
    copy.addProperty("format_version", version)
    return copy
}
