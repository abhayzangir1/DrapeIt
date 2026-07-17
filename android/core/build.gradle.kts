plugins {
    id("org.jetbrains.kotlin.jvm")
    `java-library`
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(libs.junit)
}
