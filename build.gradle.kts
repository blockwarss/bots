plugins { java }
java { toolchain.languageVersion.set(JavaLanguageVersion.of(21)) }
group = "com.nowheberg"
version = "1.1.0"
repositories { mavenLocal(); maven("https://repo.papermc.io/repository/maven-public/") }
dependencies { compileOnly("io.papermc.paper:paper-api:1.21-R0.1-SNAPSHOT") }
