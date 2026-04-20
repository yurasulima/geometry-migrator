package geometry.migration.steps

import geometry.migration.*
import geometry.version.GeometryVersion
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

class Migrate_1_12_0_To_1_14_0Test {

    private val step = Migrate_1_12_0_To_1_14_0()

    private fun migrate(json: String) = step.migrate(json.toJsonObject())

    @Test fun `step reports correct from-to versions`() {
        assertThat(step.from).isEqualTo(GeometryVersion.V1_12_0)
        assertThat(step.to).isEqualTo(GeometryVersion.V1_14_0)
    }

    @Test fun `format_version updated to 1_14_0`() {
        assertThat(migrate(minimalGeo()).str("format_version")).isEqualTo("1.14.0")
    }

    @Nested inner class UvConversion {

        @Test fun `simple array uv is converted to per-face object`() {
            val result = migrate(geoWithCube("""
                { "origin": [0,0,0], "size": [8,8,8], "uv": [0, 0] }
            """))
            val uv = result.firstCube().get("uv")
            assertThat(uv.isJsonObject).isTrue
        }

        @Test fun `converted uv object has all six faces`() {
            val result = migrate(geoWithCube("""
                { "origin": [0,0,0], "size": [8,8,8], "uv": [0, 0] }
            """))
            val uv = result.firstCube().obj("uv")
            listOf("north", "south", "east", "west", "up", "down").forEach { face ->
                assertThat(uv.has(face)).`as`("face '$face' missing").isTrue
            }
        }

        @Test fun `each face has uv and uv_size arrays`() {
            val result = migrate(geoWithCube("""
                { "origin": [0,0,0], "size": [4,6,2], "uv": [10, 20] }
            """))
            val uv = result.firstCube().obj("uv")
            listOf("north", "south", "east", "west", "up", "down").forEach { face ->
                val faceObj = uv.obj(face)
                assertThat(faceObj.arr("uv").size()).isEqualTo(2)
                assertThat(faceObj.arr("uv_size").size()).isEqualTo(2)
            }
        }

        /**
         * Verifies the standard Minecraft box UV layout for an 8x12x4 cube with `uv: [16, 16]`.
         *
         * Layout:
         *        [sz=4] [sx=8] [sz=4] [sx=8]
         *  [sz=4]  up           down
         *  [sy=12] east  north  west  south
         *
         * east:  u=16,        v=20  -> size=(4,12)
         * north: u=20,        v=20  -> size=(8,12)
         * west:  u=28,        v=20  -> size=(4,12)
         * south: u=32,        v=20  -> size=(8,12)
         * up:    u=20,        v=16  -> size=(8,4)
         * down:  u=28,        v=16  -> size=(8,4)
         */
        @Test fun `uv coordinates follow standard minecraft box-uv layout`() {
            val result = migrate(geoWithCube("""
                { "origin": [-4,12,-2], "size": [8,12,4], "uv": [16, 16] }
            """))
            val uv = result.firstCube().obj("uv")

            fun face(name: String) = uv.obj(name).arr("uv")
            fun size(name: String) = uv.obj(name).arr("uv_size")

            assertThat(face("east")[0].asFloat).isEqualTo(16f)
            assertThat(face("east")[1].asFloat).isEqualTo(20f)
            assertThat(size("east")[0].asFloat).isEqualTo(4f)
            assertThat(size("east")[1].asFloat).isEqualTo(12f)

            assertThat(face("north")[0].asFloat).isEqualTo(20f)
            assertThat(face("north")[1].asFloat).isEqualTo(20f)
            assertThat(size("north")[0].asFloat).isEqualTo(8f)
            assertThat(size("north")[1].asFloat).isEqualTo(12f)

            assertThat(face("west")[0].asFloat).isEqualTo(28f)
            assertThat(face("west")[1].asFloat).isEqualTo(20f)

            assertThat(face("south")[0].asFloat).isEqualTo(32f)
            assertThat(face("south")[1].asFloat).isEqualTo(20f)

            assertThat(face("up")[0].asFloat).isEqualTo(20f)
            assertThat(face("up")[1].asFloat).isEqualTo(16f)
            assertThat(size("up")[0].asFloat).isEqualTo(8f)
            assertThat(size("up")[1].asFloat).isEqualTo(4f)

            assertThat(face("down")[0].asFloat).isEqualTo(28f)
            assertThat(face("down")[1].asFloat).isEqualTo(16f)
        }

        @Test fun `per-face uv already present is not modified`() {
            val json = geoWithCube("""
            {
              "origin": [0,0,0], "size": [4,4,4],
              "uv": {
                "north": { "uv": [0, 0], "uv_size": [4, 4] },
                "south": { "uv": [0, 0], "uv_size": [4, 4] },
                "east":  { "uv": [0, 0], "uv_size": [4, 4] },
                "west":  { "uv": [0, 0], "uv_size": [4, 4] },
                "up":    { "uv": [0, 0], "uv_size": [4, 4] },
                "down":  { "uv": [0, 0], "uv_size": [4, 4] }
              }
            }
            """)
            val result = migrate(json)
            assertThat(result.firstCube().get("uv").isJsonObject).isTrue
            assertThat(result.firstCube().obj("uv").obj("north").arr("uv")[0].asFloat).isEqualTo(0f)
        }

        @Test fun `cube without uv field is left untouched`() {
            val result = migrate(geoWithCube("""
                { "origin": [0,0,0], "size": [4,4,4] }
            """))
            assertThat(result.firstCube().has("uv")).isFalse
        }

        @Test fun `multiple cubes in one bone all converted`() {
            val json = geoWithBone("""
            {
              "name": "body",
              "cubes": [
                { "origin": [0,0,0], "size": [4,4,4], "uv": [0, 0] },
                { "origin": [4,0,0], "size": [2,2,2], "uv": [20, 0] }
              ]
            }
            """)
            val result = migrate(json)
            val cubes = result.firstBone().arr("cubes")
            assertThat(cubes[0].asJsonObject.get("uv").isJsonObject).isTrue
            assertThat(cubes[1].asJsonObject.get("uv").isJsonObject).isTrue
        }
    }


    @Nested inner class Locators {

        @Test fun `array locator converted to object with offset`() {
            val result = migrate(geoWithBone("""
            {
              "name": "body",
              "cubes": [],
              "locators": {
                "lead": [0, 24, 0]
              }
            }
            """))
            val lead = result.firstBone().obj("locators").obj("lead")
            assertThat(lead.has("offset")).isTrue
            assertThat(lead.arr("offset")[1].asFloat).isEqualTo(24f)
        }

        @Test fun `object locator already in new format is not modified`() {
            val result = migrate(geoWithBone("""
            {
              "name": "body",
              "cubes": [],
              "locators": {
                "collar": { "offset": [0, 16, -4], "rotation": [0, 0, 0] }
              }
            }
            """))
            val collar = result.firstBone().obj("locators").obj("collar")
            assertThat(collar.has("offset")).isTrue
            assertThat(collar.has("rotation")).isTrue
        }

        @Test fun `bone without locators is unaffected`() {
            val result = migrate(geoWithBone("""{ "name": "body", "cubes": [] }"""))
            assertThat(result.firstBone().has("locators")).isFalse
        }
    }

    private fun minimalGeo() = """
    {
      "format_version": "1.12.0",
      "minecraft:geometry": [{ "description": { "identifier": "geometry.test" }, "bones": [] }]
    }
    """

    private fun geoWithCube(cubeJson: String) = geoWithBone("""
    {
      "name": "body",
      "cubes": [ $cubeJson ]
    }
    """)

    private fun geoWithBone(boneJson: String) = """
    {
      "format_version": "1.12.0",
      "minecraft:geometry": [{
        "description": { "identifier": "geometry.test", "texture_width": 64, "texture_height": 32 },
        "bones": [ $boneJson ]
      }]
    }
    """
}
