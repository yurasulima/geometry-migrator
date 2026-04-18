import java.net.URI

plugins {
    kotlin("jvm") version "1.9.23"
    `java-library`
    `maven-publish`
    signing
}


group   = "io.github.yurasulima"
version = "1.0.0"



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
            url = if (version.toString().endsWith("SNAPSHOT"))
                URI("https://s01.oss.sonatype.org/content/repositories/snapshots/")
            else
                URI("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2/")

            credentials {
                username = providers.gradleProperty("ossrhUsername")
                    .orElse(providers.environmentVariable("OSSRH_USERNAME"))
                    .orNull
                password = providers.gradleProperty("ossrhPassword")
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
