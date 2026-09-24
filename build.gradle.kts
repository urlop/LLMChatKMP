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
}

// Generated sources (Compose resource accessors, Room/KSP output) are excluded via the root
// .editorconfig's `[**/build/**.kt] ktlint = disabled` section rather than here: ktlint-gradle's
// `filter {}` DSL does not apply to the auto-discovered Kotlin Multiplatform source-set tasks.
subprojects {
    apply(plugin = "org.jlleitschuh.gradle.ktlint")
}
