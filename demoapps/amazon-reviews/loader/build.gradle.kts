plugins {
    alias(libs.plugins.kotlinJvm)
    application
    id("com.gradleup.shadow") version "8.3.9"
}

dependencies {
    implementation(project(":common"))
    implementation("io.ktor:ktor-client-core:3.1.3")
    implementation("io.ktor:ktor-client-cio:3.1.3")
    implementation("io.ktor:ktor-client-content-negotiation:3.1.3")
    implementation("io.ktor:ktor-serialization-jackson:3.1.3")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.20.1")
    implementation("org.apache.parquet:parquet-avro:1.15.1")
    implementation("org.apache.hadoop:hadoop-common:3.4.1") {
        // Minimize transitive deps for a CLI tool
        exclude(group = "org.apache.curator")
        exclude(group = "org.apache.zookeeper")
        exclude(group = "org.eclipse.jetty")
    }
    implementation("org.apache.avro:avro:1.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
    implementation("ch.qos.logback:logback-classic:1.5.21")
}

application {
    mainClass = "com.example.amazonreviews.loader.DatasetLoaderKt"
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveBaseName.set("loader")
    archiveClassifier.set("")
    mergeServiceFiles()
    // Exclude dnsjava SPI that conflicts with JDK 21 InetAddressResolver
    exclude("META-INF/services/java.net.spi.InetAddressResolverProvider")
    manifest {
        attributes["Main-Class"] = "com.example.amazonreviews.loader.DatasetLoaderKt"
    }
}

// Wire application dist tasks to depend on shadowJar since it overwrites the regular jar
tasks.named("startScripts") { dependsOn("shadowJar") }
tasks.named("distTar") { dependsOn("shadowJar") }
tasks.named("distZip") { dependsOn("shadowJar") }
