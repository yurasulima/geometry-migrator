import java.net.URI
import java.net.HttpURLConnection
import java.util.Base64
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.gradle.api.publish.maven.tasks.PublishToMavenRepository

plugins {
    kotlin("jvm") version "1.9.23"
    `java-library`
    `maven-publish`
    signing
}


group   = "io.github.yurasulima"
version = "1.0.6"



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

val centralStagingDir = layout.buildDirectory.dir("central-staging")
val centralBundleDir = layout.buildDirectory.dir("central-publishing")
val centralBundleZip = centralBundleDir.map { it.file("central-bundle.zip") }


publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = "geometry-migrator"
            from(components["java"])

            pom {
                name.set("Geometry Migrator")
                description.set(
                    "Kotlin library for migrating Minecraft Bedrock Edition .json " +
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
            name = "CentralBundle"
            url = centralStagingDir.get().asFile.toURI()
        }

        maven {
            name = "CentralSnapshots"
            url = uri("https://central.sonatype.com/api/v1/publisher/upload")
            credentials {
                username = providers.environmentVariable("OSSRH_USERNAME").orNull
                password = providers.environmentVariable("OSSRH_PASSWORD").orNull
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
    onlyIf {
        gradle.taskGraph.hasTask(":publishMavenJavaPublicationToCentralBundleRepository") ||
            gradle.taskGraph.hasTask(":publishMavenJavaPublicationToCentralSnapshotsRepository")
    }
}

val centralTokenUsername = providers.environmentVariable("OSSRH_USERNAME").orNull
val centralTokenPassword =  providers.environmentVariable("OSSRH_PASSWORD").orNull

tasks.withType<PublishToMavenRepository>().configureEach {
    onlyIf {
        when (repository.name) {
            "CentralBundle" -> !version.toString().endsWith("SNAPSHOT")
            "CentralSnapshots" -> version.toString().endsWith("SNAPSHOT")
            else -> true
        }
    }
}

val createCentralBundle = tasks.register("createCentralBundle") {
    group = "publishing"
    description = "Creates a Maven Central deployment bundle zip from the staged Maven layout."
    dependsOn("publishMavenJavaPublicationToCentralBundleRepository")
    outputs.file(centralBundleZip)

    doLast {
        val stagingRoot = centralStagingDir.get().asFile
        val bundleRoot = centralBundleDir.get().asFile
        val bundleFile = centralBundleZip.get().asFile

        bundleRoot.mkdirs()
        if (bundleFile.exists()) {
            bundleFile.delete()
        }

        ZipOutputStream(bundleFile.outputStream().buffered()).use { zip ->
            stagingRoot.walkTopDown()
                .filter { it.isFile }
                .forEach { file ->
                    val relative = file.relativeTo(stagingRoot).invariantSeparatorsPath
                    zip.putNextEntry(ZipEntry(relative))
                    file.inputStream().use { input -> input.copyTo(zip) }
                    zip.closeEntry()
                }
        }
    }
}

val publishToCentralPortal = tasks.register("publishToCentralPortal") {
    group = "publishing"
    description = "Uploads a Central deployment bundle to the Central Publisher Portal."
    dependsOn(createCentralBundle)
    onlyIf { !version.toString().endsWith("SNAPSHOT") }

    doLast {
        val username = centralTokenUsername ?: error("Missing Central Portal username/token username.")
        val password = centralTokenPassword ?: error("Missing Central Portal password/token password.")
        val bundleFile = centralBundleZip.get().asFile
        require(bundleFile.exists()) { "Central bundle not found: ${bundleFile.absolutePath}" }

        val token = Base64.getEncoder()
            .encodeToString("$username:$password".toByteArray(Charsets.UTF_8))
        val boundary = "----CodexCentral${UUID.randomUUID()}"
        val lineBreak = "\r\n"
        val endpoint = URI(
            "https://central.sonatype.com/api/v1/publisher/upload" +
                "?name=${project.name}-${project.version}&publishingType=AUTOMATIC"
        ).toURL()

        val connection = endpoint.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Authorization", "Bearer $token")
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("Content-Type", "multipart/form-data; boundary=$boundary")
        connection.doOutput = true

        connection.outputStream.buffered().use { output ->
            output.write("--$boundary$lineBreak".toByteArray(Charsets.UTF_8))
            output.write(
                ("Content-Disposition: form-data; name=\"bundle\"; filename=\"${bundleFile.name}\"$lineBreak")
                    .toByteArray(Charsets.UTF_8)
            )
            output.write("Content-Type: application/octet-stream$lineBreak$lineBreak".toByteArray(Charsets.UTF_8))
            bundleFile.inputStream().use { input -> input.copyTo(output) }
            output.write(lineBreak.toByteArray(Charsets.UTF_8))
            output.write("--$boundary--$lineBreak".toByteArray(Charsets.UTF_8))
        }

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
    description = "Publishes a release bundle directly to Maven Central via the Central Publisher Portal."
    dependsOn(publishToCentralPortal)
}

tasks.register("publishSnapshotToCentral") {
    group = "publishing"
    description = "Publishes a SNAPSHOT build to the Central snapshots repository."
    dependsOn("publishMavenJavaPublicationToCentralSnapshotsRepository")
}
