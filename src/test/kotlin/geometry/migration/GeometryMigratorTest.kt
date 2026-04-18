package geometry.migration

import geometry.version.GeometryVersion
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class GeometryMigratorTest {

    private val migrator = GeometryMigrator()

    @Nested inner class FullChain {

        @Test fun `migrates 1_8_0 to 1_21_0`() {
            val result = migrator.migrate(geo_1_8_0.toJsonObject())
            assertThat(result.str("format_version")).isEqualTo("1.21.0")
        }

        @Test fun `result has minecraft_geometry array`() {
            val result = migrator.migrate(geo_1_8_0.toJsonObject())
            assertThat(result.has("minecraft:geometry")).isTrue
        }

        @Test fun `identifier preserved through full chain`() {
            val result = migrator.migrate(geo_1_8_0.toJsonObject())
            assertThat(result.firstGeometry().obj("description").str("identifier"))
                .isEqualTo("geometry.zombie")
        }

        @Test fun `texture size preserved through full chain`() {
            val result = migrator.migrate(geo_1_8_0.toJsonObject())
            val desc = result.firstGeometry().obj("description")
            assertThat(desc.int("texture_width")).isEqualTo(64)
            assertThat(desc.int("texture_height")).isEqualTo(32)
        }

        @Test fun `bones count preserved through full chain`() {
            val result = migrator.migrate(geo_1_8_0.toJsonObject())
            assertThat(result.firstGeometry().arr("bones").size()).isEqualTo(2)
        }

        @Test fun `simple uv converted to per-face in full chain`() {
            val result = migrator.migrate(geo_1_8_0.toJsonObject())
            val uv = result.firstCube().get("uv")
            assertThat(uv.isJsonObject).isTrue
        }

        @Test fun `removed bone fields absent after full chain`() {
            val result = migrator.migrate(geo_1_8_0.toJsonObject())
            val bone = result.firstBone()
            assertThat(bone.has("reset")).isFalse
            assertThat(bone.has("neverRender")).isFalse
            assertThat(bone.has("bind_pose_rotation")).isFalse
        }

        @Test fun `bone name preserved through full chain`() {
            val result = migrator.migrate(geo_1_8_0.toJsonObject())
            assertThat(result.firstBone().str("name")).isEqualTo("body")
        }
    }

    @Nested inner class PartialMigration {

        @Test fun `1_8_0 to 1_12_0 stops after first step`() {
            val result = migrator.migrate(geo_1_8_0.toJsonObject(), GeometryVersion.V1_12_0)
            assertThat(result.str("format_version")).isEqualTo("1.12.0")
        }

        @Test fun `1_8_0 to 1_14_0 applies two steps`() {
            val result = migrator.migrate(geo_1_8_0.toJsonObject(), GeometryVersion.V1_14_0)
            assertThat(result.str("format_version")).isEqualTo("1.14.0")
            assertThat(result.firstCube().get("uv").isJsonObject).isTrue
        }

        @Test fun `1_12_0 to 1_16_0 skips 1_8_0 step`() {
            val input = geo_1_12_0.toJsonObject()
            val result = migrator.migrate(input, GeometryVersion.V1_16_0)
            assertThat(result.str("format_version")).isEqualTo("1.16.0")
            assertThat(result.firstGeometry().obj("description").str("identifier"))
                .isEqualTo("geometry.cat")
        }

        @Test fun `already at target version returns same content`() {
            val input = geo_1_21_0.toJsonObject()
            val result = migrator.migrate(input, GeometryVersion.V1_21_0)
            assertThat(result.str("format_version")).isEqualTo("1.21.0")
            assertThat(result.firstGeometry().obj("description").str("identifier"))
                .isEqualTo("geometry.player")
        }
    }

    @Nested inner class JsonStringApi {

        @Test fun `migrateJson returns valid json string`() {
            val result = migrator.migrateJson(geo_1_8_0)
            assertThat(result).contains("\"format_version\"")
            assertThat(result).contains("1.21.0")
        }

        @Test fun `migrateJson to specific version`() {
            val result = migrator.migrateJson(geo_1_8_0, GeometryVersion.V1_16_0)
            assertThat(result).contains("1.16.0")
        }

        @Test fun `migrateJson output is pretty-printed`() {
            val result = migrator.migrateJson(geo_1_8_0)
            assertThat(result).contains("\n")
        }
    }

    @Nested inner class ErrorHandling {

        @Test fun `throws on missing format_version`() {
            val json = """{ "minecraft:geometry": [] }""".toJsonObject()
            assertThatThrownBy { migrator.migrate(json) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("format_version")
        }

        @Test fun `throws on unknown version string`() {
            val json = """{ "format_version": "0.0.1", "minecraft:geometry": [] }""".toJsonObject()
            assertThatThrownBy { migrator.migrate(json) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("0.0.1")
        }

        @Test fun `throws when migrating backwards`() {
            val json = geo_1_21_0.toJsonObject()
            assertThatThrownBy { migrator.migrate(json, GeometryVersion.V1_12_0) }
                .isInstanceOf(IllegalArgumentException::class.java)
                .hasMessageContaining("backwards")
        }
    }

    private val geo_1_8_0 = """
    {
      "format_version": "1.8.0",
      "geometry.zombie": {
        "texturewidth": 64,
        "textureheight": 32,
        "visible_bounds_width": 2.0,
        "visible_bounds_height": 2.5,
        "visible_bounds_offset": [0, 1, 0],
        "bones": [
          {
            "name": "body",
            "pivot": [0, 24, 0],
            "reset": true,
            "neverRender": false,
            "bind_pose_rotation": [0, 0, 0],
            "cubes": [
              { "origin": [-4, 12, -2], "size": [8, 12, 4], "uv": [16, 16] }
            ],
            "locators": { "lead": [0, 24, 0] }
          },
          {
            "name": "head",
            "parent": "body",
            "pivot": [0, 24, 0],
            "cubes": [
              { "origin": [-4, 24, -4], "size": [8, 8, 8], "uv": [0, 0] }
            ]
          }
        ]
      }
    }
    """

    private val geo_1_12_0 = """
    {
      "format_version": "1.12.0",
      "minecraft:geometry": [{
        "description": {
          "identifier": "geometry.cat",
          "texture_width": 64,
          "texture_height": 32
        },
        "bones": [{
          "name": "body",
          "pivot": [0, 16, 0],
          "cubes": [
            { "origin": [-3, 8, -4], "size": [6, 10, 7], "uv": [12, 8] }
          ]
        }]
      }]
    }
    """

    private val geo_1_21_0 = """
    {
      "format_version": "1.21.0",
      "minecraft:geometry": [{
        "description": {
          "identifier": "geometry.player",
          "texture_width": 64,
          "texture_height": 64
        },
        "bones": []
      }]
    }
    """
}
