plugins {
    `java-library`
    kotlin("jvm") version "2.2.21"
    kotlin("kapt") version "2.2.21"
}

group = "com.example.hevmesh"

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.micronaut:micronaut-inject:4.6.1")
    implementation("io.micronaut:micronaut-http:4.6.1")

    implementation("io.ktor:ktor-client-core:3.1.3")
    implementation("io.ktor:ktor-client-cio:3.1.3")
    implementation("io.ktor:ktor-client-content-negotiation:3.1.3")
    implementation("io.ktor:ktor-serialization-jackson:3.1.3")

    kapt("io.micronaut:micronaut-inject-java:4.6.1")
    kapt("io.micronaut.kotlin:micronaut-kotlin-runtime:4.7.0")
}
