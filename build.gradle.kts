import org.gradle.api.JavaVersion.VERSION_1_8
import org.jetbrains.intellij.tasks.PatchPluginXmlTask
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.lang.Runtime.getRuntime
import java.lang.System.getenv

buildscript {
    repositories {
        mavenCentral()
        maven("https://www.jetbrains.com/intellij-repository/releases")
    }
    dependencies {
        classpath(kotlin("gradle-plugin", "1.3.41"))
    }
}

val commitHash by lazy {
    val output: String
    val process = getRuntime().exec("git rev-parse --short HEAD")
    process.waitFor()
    output = process.inputStream.use {
        it.bufferedReader().use {
            it.readText()
        }
    }
    process.destroy()
    output.trim()
}

val isCI = !getenv("CI").isNullOrBlank()

val pluginComingVersion = "0.0.1"
val pluginVersion = if (isCI) "$pluginComingVersion-$commitHash" else pluginComingVersion
val packageName = "com.ppismerov.ksvu"

group = packageName
version = pluginVersion

plugins {
    java
    id("org.jetbrains.intellij") version "0.4.9"
    kotlin("jvm") version "1.3.41"
}

fun fromToolbox(root: String, ide: String) = file(root)
        .resolve(ide)
        .takeIf { it.exists() }
        ?.resolve("ch-0")
        ?.listFiles()
        .orEmpty()
        .filterNotNull()
        .filter { it.isDirectory }
        .maxBy {
            val (major, minor, patch) = it.name.split(".")
            "%5s%5s%5s".format(major, minor, patch)
        }
        ?.also { println("Picked: $it") }

java {
    sourceCompatibility = VERSION_1_8
    targetCompatibility = VERSION_1_8
}

repositories {
    mavenCentral()
    jcenter()
    maven("https://www.jetbrains.com/intellij-repository/releases")
}

dependencies {
    val junitVersion = "5.5.0"
    implementation(kotlin("stdlib-jdk8"))

    testCompile(kotlin("test-junit"))
    testCompile("org.junit.jupiter:junit-jupiter-api:$junitVersion")
    testCompile("org.junit.jupiter:junit-jupiter-params:$junitVersion")
    testCompile("org.junit.jupiter:junit-jupiter-engine:$junitVersion")
}

// See https://github.com/JetBrains/gradle-intellij-plugin/
intellij {
    updateSinceUntilBuild = false
    instrumentCode = true
    pluginName = "kotlin-serial-version-uid-plugin"
//    version = "2018.1"
//    type = "IC"
    val user = System.getProperty("user.name")
    val os = System.getProperty("os.name")
    val root = when {
        os.startsWith("Windows") -> "C:\\Users\\$user\\AppData\\Local\\JetBrains\\Toolbox\\apps"
        os == "Linux" -> "/home/$user/.local/share/JetBrains/Toolbox/apps"
        else -> return@intellij
    }
    val intellijPath = sequenceOf("IDEA-U", "IDEA-C", "IDEA-C-JDK11", "IDEA-JDK11")
            .mapNotNull { fromToolbox(root, it) }.firstOrNull()
    intellijPath?.absolutePath?.let { localPath = it }
//    setPlugins("org.jetbrains.kotlin:1.3.41-release-IJ2019.2-1")
    setPlugins("Kotlin", "java")
}

configure<JavaPluginConvention> {
    sourceCompatibility = VERSION_1_8
    targetCompatibility = VERSION_1_8
}

sourceSets {
    main {
        withConvention(KotlinSourceSet::class) {
            listOf(java, kotlin).forEach { it.srcDirs("kotlin") }
        }
        resources.srcDir("resources")
    }

    test {
        withConvention(KotlinSourceSet::class) {
            listOf(java, kotlin).forEach { it.srcDirs("kotlin") }
        }
        resources.srcDir("resources")
    }
}

tasks {
    getByName<PatchPluginXmlTask>("patchPluginXml") {
        changeNotes(file("change-notes.html").readText())
        pluginDescription(file("description.html").readText())
        version(pluginVersion)
        pluginId(packageName)
    }

    withType<KotlinCompile> {
        targetCompatibility = VERSION_1_8.toString()
        sourceCompatibility = VERSION_1_8.toString()
        kotlinOptions {
            jvmTarget = VERSION_1_8.toString()
            languageVersion = "1.3"
            apiVersion = "1.3"
        }
    }
}
