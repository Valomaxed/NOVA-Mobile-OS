plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(libs.kotlin.stdlib)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlin.test.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

tasks.test {
    useJUnitPlatform { excludeTags("benchmark") }
}

tasks.register<Test>("vectorSearchBenchmark") {
    description = "Compare exact vector search algorithms on the host JVM."
    group = "verification"
    testClassesDirs = sourceSets["test"].output.classesDirs
    classpath = sourceSets["test"].runtimeClasspath
    useJUnitPlatform { includeTags("benchmark") }
    systemProperty("nova.benchmark.report", layout.buildDirectory.file("reports/vector-search-benchmark.csv").get().asFile.absolutePath)
    outputs.upToDateWhen { false }
}
