package geometry.version

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test

class GeometryVersionTest {

    @Test fun `fromString returns correct version for each known string`() {
        mapOf(
            "1.8.0"   to GeometryVersion.V1_8_0,
            "1.12.0"  to GeometryVersion.V1_12_0,
            "1.14.0"  to GeometryVersion.V1_14_0,
            "1.16.0"  to GeometryVersion.V1_16_0,
            "1.19.30" to GeometryVersion.V1_19_30,
            "1.21.0"  to GeometryVersion.V1_21_0,
        ).forEach { (str, expected) ->
            assertThat(GeometryVersion.fromString(str))
                .`as`("fromString(\"$str\")")
                .isEqualTo(expected)
        }
    }

    @Test fun `fromString throws on unknown version`() {
        assertThatThrownBy { GeometryVersion.fromString("9.9.9") }
            .isInstanceOf(IllegalArgumentException::class.java)
            .hasMessageContaining("9.9.9")
    }

    @Test fun `versions are ordered oldest to newest via ordinal`() {
        val versions = GeometryVersion.entries
        for (i in 0 until versions.size - 1) {
            assertThat(versions[i].ordinal)
                .`as`("${versions[i]} should come before ${versions[i + 1]}")
                .isLessThan(versions[i + 1].ordinal)
        }
    }

    @Test fun `versionString values are non-empty`() {
        GeometryVersion.entries.forEach {
            assertThat(it.versionString).isNotBlank
        }
    }
}
