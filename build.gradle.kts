plugins {
    java
}

group = "dev.imagecanvas"
version = "1.0.0"

// Paper-API-Version; per "gradle build -PpaperApi=26.3.build.+" ueberschreibbar
val paperApi = (findProperty("paperApi") as String?) ?: "26.2.build.+"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:$paperApi")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release.set(25)
}

tasks.processResources {
    inputs.property("version", project.version)
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.jar {
    archiveFileName.set("ImageCanvas-${project.version}.jar")
}
