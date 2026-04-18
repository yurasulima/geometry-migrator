package geometry.migration.steps

import geometry.migration.*
import geometry.version.GeometryVersion
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

/**
 * Tests placeholder version-bump steps to ensure the version changes
 * while all other content remains intact.
 */
class VersionBumpStepsTest {

    private val step_14_to_16   = Migrate_1_14_0_To_1_16_0()
    private val step_16_to_1930 = Migrate_1_16_0_To_1_19_30()
    private val step_1930_to_21 = Migrate_1_19_30_To_1_21_0()

    @Test fun `1_14_to_1_16 - correct versions`() {
        assertThat(step_14_to_16.from).isEqualTo(GeometryVersion.V1_14_0)
        assertThat(step_14_to_16.to).isEqualTo(GeometryVersion.V1_16_0)
    }

    @Test fun `1_14_to_1_16 - format_version updated`() {
        val result = step_14_to_16.migrate(geo("1.14.0"))
        assertThat(result.str("format_version")).isEqualTo("1.16.0")
    }

    @Test fun `1_14_to_1_16 - identifier preserved`() {
        val result = step_14_to_16.migrate(geo("1.14.0"))
        assertThat(result.firstGeometry().obj("description").str("identifier"))
            .isEqualTo("geometry.test")
    }

    @Test fun `1_14_to_1_16 - bone name preserved`() {
        val result = step_14_to_16.migrate(geoWithBone("1.14.0", "spine").toJsonObject())
        assertThat(result.firstBone().str("name")).isEqualTo("spine")
    }

    @Test fun `1_16_to_1930 - correct versions`() {
        assertThat(step_16_to_1930.from).isEqualTo(GeometryVersion.V1_16_0)
        assertThat(step_16_to_1930.to).isEqualTo(GeometryVersion.V1_19_30)
    }

    @Test fun `1_16_to_1930 - format_version updated`() {
        assertThat(step_16_to_1930.migrate(geo("1.16.0")).str("format_version"))
            .isEqualTo("1.19.30")
    }

    @Test fun `1_16_to_1930 - binding field preserved if present`() {
        val json = geoWithBone("1.16.0", "rightArm", binding = "q.item_slot_to_bone_name(c.item_slot)")
        val result = step_16_to_1930.migrate(json.toJsonObject())
        assertThat(result.firstBone().str("binding"))
            .isEqualTo("q.item_slot_to_bone_name(c.item_slot)")
    }

    @Test fun `1_16_to_1930 - texture size preserved`() {
        val result = step_16_to_1930.migrate(geo("1.16.0", textureW = 128, textureH = 64))
        val desc = result.firstGeometry().obj("description")
        assertThat(desc.int("texture_width")).isEqualTo(128)
        assertThat(desc.int("texture_height")).isEqualTo(64)
    }

    @Test fun `1930_to_21 - correct versions`() {
        assertThat(step_1930_to_21.from).isEqualTo(GeometryVersion.V1_19_30)
        assertThat(step_1930_to_21.to).isEqualTo(GeometryVersion.V1_21_0)
    }

    @Test fun `1930_to_21 - format_version updated`() {
        assertThat(step_1930_to_21.migrate(geo("1.19.30")).str("format_version"))
            .isEqualTo("1.21.0")
    }

    @Test fun `1930_to_21 - per-face uv_rotation preserved if already present`() {
        val json = """
        {
          "format_version": "1.19.30",
          "minecraft:geometry": [{
            "description": { "identifier": "geometry.test" },
            "bones": [{
              "name": "body",
              "cubes": [{
                "origin": [0,0,0], "size": [4,4,4],
                "uv": {
                  "north": { "uv": [0,0], "uv_size": [4,4], "uv_rotation": 90 }
                }
              }]
            }]
          }]
        }
        """.toJsonObject()
        val result = step_1930_to_21.migrate(json)
        val northRot = result.firstCube().obj("uv").obj("north").int("uv_rotation")
        assertThat(northRot).isEqualTo(90)
    }

    @Test fun `1930_to_21 - item_display_transforms preserved if already present`() {
        val json = """
        {
          "format_version": "1.19.30",
          "minecraft:geometry": [{
            "description": { "identifier": "geometry.item" },
            "item_display_transforms": {
              "gui": { "scale": [0.625, 0.625, 0.625] }
            },
            "bones": []
          }]
        }
        """.toJsonObject()
        val result = step_1930_to_21.migrate(json)
        assertThat(result.firstGeometry().has("item_display_transforms")).isTrue
    }

    private fun geo(version: String, textureW: Int = 64, textureH: Int = 32) = """
    {
      "format_version": "$version",
      "minecraft:geometry": [{
        "description": {
          "identifier": "geometry.test",
          "texture_width": $textureW,
          "texture_height": $textureH
        },
        "bones": []
      }]
    }
    """.toJsonObject()

    private fun geoWithBone(
        version: String,
        boneName: String,
        binding: String? = null
    ): String {
        val bindingField = if (binding != null) """"binding": "$binding",""" else ""
        return """
        {
          "format_version": "$version",
          "minecraft:geometry": [{
            "description": { "identifier": "geometry.test" },
            "bones": [{
              "name": "$boneName",
              $bindingField
              "cubes": []
            }]
          }]
        }
        """
    }
}
