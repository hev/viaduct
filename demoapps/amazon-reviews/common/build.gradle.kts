plugins {
    `java-library`
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinKapt)
}

dependencies {
    implementation(libs.micronaut.inject)
    implementation(libs.micronaut.http)

    implementation("io.ktor:ktor-client-core:3.1.3")
    implementation("io.ktor:ktor-client-cio:3.1.3")
    implementation("io.ktor:ktor-client-content-negotiation:3.1.3")
    implementation("io.ktor:ktor-serialization-jackson:3.1.3")

    kapt(libs.micronaut.inject.java)
    kapt(libs.micronaut.inject.kotlin)
}
