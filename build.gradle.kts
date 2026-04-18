import java.net.URI
import java.net.HttpURLConnection
import java.util.Base64

plugins {
    kotlin("jvm") version "1.9.23"
    `java-library`
    `maven-publish`
    signing
}


group   = "io.github.yurasulima"
version = "1.0.5"



kotlin { jvmToolchain(17) }

java {
    withJavadocJar()
    withSourcesJar()
}


repositories {
    mavenCentral()
}


dependencies {
    api("com.google.code.gson:gson:2.10.1")
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.assertj:assertj-core:3.27.7")
}

tasks.test {
    useJUnitPlatform()
}


publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "geometry-migrator"
            from(components["java"])

            pom {
                name.set("Geometry Migrator")
                description.set(
                    "Kotlin library for migrating Minecraft Bedrock Edition .geo.json " +
                    "geometry files across all supported format versions (1.8.0 – 1.21.0)."
                )
                url.set("https://github.com/yurasulima/geometry-migrator")

                licenses {
                    license {
                        name.set("MIT License")
                        url.set("https://opensource.org/licenses/MIT")
                    }
                }

                developers {
                    developer {
                        id.set("yurasulima")
                        name.set("Yurii Sulyma")
                        email.set("yuriisulyma@gmail.com")
                    }
                }

                scm {
                    connection.set("scm:git:git://github.com/yurasulima/geometry-migrator.git")
                    developerConnection.set("scm:git:ssh://github.com/yurasulima/geometry-migrator.git")
                    url.set("https://github.com/yurasulima/geometry-migrator/tree/main")
                }
            }
        }
    }

    repositories {
        maven {
            name = "OSSRH"
            url = uri("https://central.sonatype.com/api/v1/publisher/upload")
            credentials {
                username = providers.gradleProperty("centralUsername")
                    .orElse(providers.gradleProperty("ossrhUsername"))
                    .orElse(providers.environmentVariable("OSSRH_USERNAME"))
                    .orNull
                password = providers.gradleProperty("centralPassword")
                    .orElse(providers.gradleProperty("ossrhPassword"))
                    .orElse(providers.environmentVariable("OSSRH_PASSWORD"))
                    .orNull
            }
        }
    }
}

signing {
    val signingKey = providers.environmentVariable("GPG_SIGNING_KEY").orNull
    val signingPasswd = providers.environmentVariable("GPG_PASSPHRASE").orNull

    if (signingKey != null && signingPasswd != null) {
        useInMemoryPgpKeys(signingKey, signingPasswd)
        sign(publishing.publications["mavenJava"])
    }
}

tasks.withType<Sign>().configureEach {
    onlyIf { gradle.taskGraph.hasTask(":publishMavenJavaPublicationToOSSRHRepository") }
}

val centralTokenUsername = providers.gradleProperty("centralUsername")
    .orElse(providers.gradleProperty("ossrhUsername"))
    .orElse(providers.environmentVariable("CENTRAL_USERNAME"))
    .orElse(providers.environmentVariable("CENTRAL_TOKEN_USERNAME"))
    .orElse(providers.environmentVariable("OSSRH_USERNAME"))

val centralTokenPassword = providers.gradleProperty("centralPassword")
    .orElse(providers.gradleProperty("ossrhPassword"))
    .orElse(providers.environmentVariable("CENTRAL_PASSWORD"))
    .orElse(providers.environmentVariable("CENTRAL_TOKEN_PASSWORD"))
    .orElse(providers.environmentVariable("OSSRH_PASSWORD"))

val publishToCentralPortal = tasks.register("publishToCentralPortal") {
    group = "publishing"
    description = "Uploads the staged release from the OSSRH compatibility API into the Central Publisher Portal."
    dependsOn("publishMavenJavaPublicationToOSSRHRepository")
    onlyIf { !version.toString().endsWith("SNAPSHOT") }

    doLast {
        val username = centralTokenUsername.orNull
            ?: error("Missing Central Portal username/token username. Set centralUsername or CENTRAL_TOKEN_USERNAME.")
        val password = centralTokenPassword.orNull
            ?: error("Missing Central Portal password/token password. Set centralPassword or CENTRAL_TOKEN_PASSWORD.")

        val token = Base64.getEncoder()
            .encodeToString("$username:$password".toByteArray(Charsets.UTF_8))
        val namespace = project.group.toString()
        val endpoint = URI(
            "https://ossrh-staging-api.central.sonatype.com/manual/upload/defaultRepository/" +
                "$namespace?publishing_type=automatic"
        ).toURL()

        val connection = endpoint.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.setRequestProperty("Accept", "application/json")
        connection.doOutput = false

        val status = connection.responseCode
        if (status !in 200..299) {
            val body = runCatching {
                connection.errorStream?.bufferedReader()?.use { it.readText() }
            }.getOrNull()
            error("Central Portal upload failed with HTTP $status${if (!body.isNullOrBlank()) ": $body" else ""}")
        }
    }
}

tasks.register("releaseToCentral") {
    group = "publishing"
    description = "Publishes the release and promotes it to Maven Central via the Central Publisher Portal."
    dependsOn(publishToCentralPortal)
}
