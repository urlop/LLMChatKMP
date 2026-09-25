plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.androidMultiplatformLibrary) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinJvm) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.kotlinSerialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.room) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.kover)
}

// Generated sources (Compose resource accessors, Room/KSP output) are excluded via the root
// .editorconfig's `[**/build/**.kt] ktlint = disabled` section rather than here: ktlint-gradle's
// `filter {}` DSL does not apply to the auto-discovered Kotlin Multiplatform source-set tasks.
subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
}

// Merged coverage across the KMP modules that hold the real logic (presentation stays in
// `shared`, domain/data are their own modules) -- Kover only instruments JVM/Android bytecode,
// so this reports JVM test coverage; iOS klibs aren't covered.
dependencies {
    kover(project(":shared"))
    kover(project(":shared:domain"))
    kover(project(":shared:data"))
}
