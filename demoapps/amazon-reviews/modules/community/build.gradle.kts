plugins {
    `java-library`
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.kotlinKapt)
    alias(libs.plugins.viaduct.module)
}

viaductModule {
    modulePackageSuffix.set("community")
}

dependencies {
    api(libs.viaduct.api)
    implementation(libs.viaduct.runtime)
    implementation(project(":common"))
    implementation(libs.jackson.module.kotlin)
    implementation(libs.micronaut.inject)
    kapt(libs.micronaut.inject.java)
    kapt(libs.micronaut.inject.kotlin)
}
