plugins {
    kotlin("jvm") version "2.0.20"
    id("io.github.goooler.shadow") version "8.1.8"
}

group = "co.sakurastudios"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven("https://oss.sonatype.org/content/groups/public/") {
        name = "sonatype"
    }
    maven("https://jitpack.io") {
        name = "jitpack"
    }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    compileOnly("com.github.NEZNAMY:TAB-API:5.0.2")
}

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks {
    shadowJar {
        archiveClassifier.set("")  // Removes the "-all" suffix

        // Relocate all Kotlin dependencies
        relocate("kotlin", "co.sakurastudios.manhuntgg.shaded.kotlin")
        relocate("kotlinx", "co.sakurastudios.manhuntgg.shaded.kotlinx")
        relocate("org.jetbrains", "co.sakurastudios.manhuntgg.shaded.org.jetbrains")

        // Ensure all Kotlin dependencies are included
        dependencies {
            include(dependency("org.jetbrains.kotlin:.*"))
            include(dependency("org.jetbrains.kotlinx:.*"))
        }

        minimize()
    }

    build {
        dependsOn(shadowJar)
    }

    processResources {
        val props = mapOf("version" to version)
        inputs.properties(props)
        filteringCharset = "UTF-8"
        filesMatching("plugin.yml") {
            expand(props)
        }
    }
}