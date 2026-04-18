package geometry.migration.steps

import geometry.migration.*
import geometry.version.GeometryVersion
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class Migrate_1_8_0_To_1_12_0Test {

    private val step = Migrate_1_8_0_To_1_12_0()

    private fun migrate(json: String) = step.migrate(json.toJsonObject())

    @Test fun `step reports correct from-to versions`() {
        assertThat(step.from).isEqualTo(GeometryVersion.V1_8_0)
        assertThat(step.to).isEqualTo(GeometryVersion.V1_12_0)
    }

    @Test fun `format_version is updated to 1_12_0`() {
        val result = migrate(minimalGeo("geometry.test"))
        assertThat(result.str("format_version")).isEqualTo("1.12.0")
    }

    @Nested inner class TopLevelStructure {

        @Test fun `root contains minecraft_geometry array`() {
            val result = migrate(minimalGeo("geometry.foo"))
            assertThat(result.has("minecraft:geometry")).isTrue
        }

        @Test fun `old geometry key is not present in result`() {
            val result = migrate(minimalGeo("geometry.foo"))
            assertThat(result.has("geometry.foo")).isFalse
        }

        @Test fun `multiple geometry keys all converted to array entries`() {
            val json = """
            {
              "format_version": "1.8.0",
              "geometry.zombie": { "texturewidth": 64, "textureheight": 32, "bones": [] },
              "geometry.zombie.armor": { "texturewidth": 64, "textureheight": 32, "bones": [] }
            }
            """
            val result = migrate(json)
            assertThat(result.arr("minecraft:geometry").size()).isEqualTo(2)
        }

        @Test fun `debug flag preserved at root level`() {
            val json = """
            {
              "format_version": "1.8.0",
              "debug": true,
              "geometry.test": { "texturewidth": 16, "textureheight": 16, "bones": [] }
            }
            """
            assertThat(migrate(json).bool("debug")).isTrue
        }
    }

    @Nested inner class Description {

        @Test fun `identifier equals original geometry key`() {
            val result = migrate(minimalGeo("geometry.pig"))
            assertThat(result.firstGeometry().obj("description").str("identifier"))
                .isEqualTo("geometry.pig")
        }

        @Test fun `texturewidth renamed to texture_width inside description`() {
            val result = migrate(geoWithTexture(64, 32))
            assertThat(result.firstGeometry().obj("description").int("texture_width"))
                .isEqualTo(64)
        }

        @Test fun `textureheight renamed to texture_height inside description`() {
            val result = migrate(geoWithTexture(64, 32))
            assertThat(result.firstGeometry().obj("description").int("texture_height"))
                .isEqualTo(32)
        }

        @Test fun `visible_bounds_width moved to description`() {
            val json = """
            {
              "format_version": "1.8.0",
              "geometry.test": {
                "texturewidth": 16, "textureheight": 16,
                "visible_bounds_width": 2.5,
                "bones": []
              }
            }
            """
            assertThat(migrate(json).firstGeometry().obj("description").float("visible_bounds_width"))
                .isEqualTo(2.5f)
        }

        @Test fun `visible_bounds_height moved to description`() {
            val json = """
            {
              "format_version": "1.8.0",
              "geometry.test": {
                "texturewidth": 16, "textureheight": 16,
                "visible_bounds_height": 3.0,
                "bones": []
              }
            }
            """
            assertThat(migrate(json).firstGeometry().obj("description").float("visible_bounds_height"))
                .isEqualTo(3.0f)
        }

        @Test fun `visible_bounds_offset moved to description`() {
            val json = """
            {
              "format_version": "1.8.0",
              "geometry.test": {
                "texturewidth": 16, "textureheight": 16,
                "visible_bounds_offset": [0, 1, 0],
                "bones": []
              }
            }
            """
            val offset = migrate(json).firstGeometry().obj("description").arr("visible_bounds_offset")
            assertThat(offset[1].asFloat).isEqualTo(1.0f)
        }
    }

    @Nested inner class RemovedBoneFields {

        @Test fun `reset is removed from bones`() {
            val result = migrate(geoWithBone("""{ "name": "body", "reset": true }"""))
            assertThat(result.firstBone().has("reset")).isFalse
        }

        @Test fun `neverRender is removed from bones`() {
            val result = migrate(geoWithBone("""{ "name": "body", "neverRender": false }"""))
            assertThat(result.firstBone().has("neverRender")).isFalse
        }

        @Test fun `bind_pose_rotation is removed from bones`() {
            val result = migrate(geoWithBone("""{ "name": "body", "bind_pose_rotation": [0,0,0] }"""))
            assertThat(result.firstBone().has("bind_pose_rotation")).isFalse
        }

        @Test fun `valid bone fields are preserved`() {
            val result = migrate(geoWithBone("""
            {
              "name": "head",
              "parent": "body",
              "pivot": [0, 24, 0],
              "rotation": [0, 0, 0],
              "mirror": true,
              "inflate": 0.5
            }
            """))
            val bone = result.firstBone()
            assertThat(bone.str("name")).isEqualTo("head")
            assertThat(bone.str("parent")).isEqualTo("body")
            assertThat(bone.bool("mirror")).isTrue
            assertThat(bone.float("inflate")).isEqualTo(0.5f)
        }
    }

    @Test fun `cape field is preserved`() {
        val json = """
        {
          "format_version": "1.8.0",
          "geometry.player": {
            "texturewidth": 64, "textureheight": 64,
            "cape": "cape_classic",
            "bones": []
          }
        }
        """
        assertThat(migrate(json).firstGeometry().str("cape")).isEqualTo("cape_classic")
    }

    private fun minimalGeo(name: String) = """
        {
          "format_version": "1.8.0",
          "$name": { "texturewidth": 16, "textureheight": 16, "bones": [] }
        }
    """

    private fun geoWithTexture(w: Int, h: Int) = """
        {
          "format_version": "1.8.0",
          "geometry.test": { "texturewidth": $w, "textureheight": $h, "bones": [] }
        }
    """

    private fun geoWithBone(boneJson: String) = """
        {
          "format_version": "1.8.0",
          "geometry.test": {
            "texturewidth": 16, "textureheight": 16,
            "bones": [ $boneJson ]
          }
        }
    """
}
