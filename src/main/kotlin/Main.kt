import geometry.migration.GeometryMigrator
import geometry.version.GeometryVersion

fun main() {
    val migrator = GeometryMigrator()

    val old_1_8_0 = """
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
              {
                "origin": [-4, 12, -2],
                "size": [8, 12, 4],
                "uv": [16, 16]
              }
            ],
            "locators": {
              "lead": [0, 24, 0]
            }
          },
          {
            "name": "head",
            "parent": "body",
            "pivot": [0, 24, 0],
            "cubes": [
              {
                "origin": [-4, 24, -4],
                "size": [8, 8, 8],
                "uv": [0, 0]
              }
            ]
          }
        ]
      }
    }
    """.trimIndent()

    println("=== 1.8.0 → 1.21.0 ===")
    println(migrator.migrateJson(old_1_8_0))

    println("\n=== 1.8.0 → 1.14.0 (тільки часткова міграція) ===")
    println(migrator.migrateJson(old_1_8_0, GeometryVersion.V1_14_0))

    val old_1_12_0 = """
    {
      "format_version": "1.12.0",
      "minecraft:geometry": [
        {
          "description": {
            "identifier": "geometry.cat",
            "texture_width": 64,
            "texture_height": 32
          },
          "bones": [
            {
              "name": "body",
              "pivot": [0, 16, 0],
              "cubes": [
                {
                  "origin": [-3, 8, -4],
                  "size": [6, 10, 7],
                  "uv": [12, 8]
                }
              ],
              "locators": {
                "collar": [0, 16, -4]
              }
            }
          ]
        }
      ]
    }
    """.trimIndent()

    println("\n=== 1.12.0 → 1.21.0 ===")
    println(migrator.migrateJson(old_1_12_0))

    val already_1_21_0 = """
    {
      "format_version": "1.21.0",
      "minecraft:geometry": [
        {
          "description": {
            "identifier": "geometry.player",
            "texture_width": 64,
            "texture_height": 64
          },
          "bones": []
        }
      ]
    }
    """.trimIndent()

    println("\n=== 1.21.0 → 1.21.0 (нічого не змінюється) ===")
    println(migrator.migrateJson(already_1_21_0))
}
