package geometry.version

enum class GeometryVersion(val versionString: String) {
    V1_8_0("1.8.0"),
    V1_12_0("1.12.0"),
    V1_14_0("1.14.0"),
    V1_16_0("1.16.0"),
    V1_19_30("1.19.30"),
    V1_21_0("1.21.0");

    companion object {
        fun fromString(v: String): GeometryVersion =
            entries.firstOrNull { it.versionString == v }
                ?: throw IllegalArgumentException("Unknown geometry version: \"$v\". Known versions: ${entries.map { it.versionString }}")
    }
}
