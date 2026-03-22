plugins {
    kotlin("jvm") version "2.0.21"
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "br.com.useinet.finance"
version = "1.0.0"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

repositories {
    mavenCentral()
    google()
}

dependencies {
    implementation("com.google.cloud.functions:functions-framework-api:1.0.4")
    implementation("com.google.apis:google-api-services-sqladmin:v1-rev20250526-2.0.0")
    implementation("com.google.cloud:google-cloud-bigquery:2.38.2")
    implementation("com.google.auth:google-auth-library-oauth2-http:1.19.0")
    implementation("com.google.code.gson:gson:2.10.1")
}

tasks.shadowJar {
    archiveClassifier = ""
    mergeServiceFiles()
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
