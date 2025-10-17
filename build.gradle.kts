plugins {
    java
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

group = "com.nowheberg"
version = "1.0.0"

repositories {
    mavenLocal()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.org/repository/maven-public/") // Citizens
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT")
    compileOnly("net.citizensnpcs:citizens-main:2.0.33-SNAPSHOT") // optionnel
}

tasks.processResources {
    filesMatching("plugin.yml") {
        expand("version" to project.version)
    }
}
