plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
}

dependencies {
    // libGDX
    api(libs.gdx)

    // FreeType for smooth fonts
    api(libs.gdx.freetype)

    // KTX
    api(libs.ktx.app)
    api(libs.ktx.graphics)
    api(libs.ktx.assets)
    api(libs.ktx.math)
    api(libs.ktx.scene2d)

    // Testing
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // libGDX test infrastructure (Pixmap requires native libraries)
    testImplementation("com.badlogicgames.gdx:gdx-backend-lwjgl3:${libs.versions.gdx.get()}")
    testRuntimeOnly("com.badlogicgames.gdx:gdx-platform:${libs.versions.gdx.get()}:natives-desktop")
    testRuntimeOnly("com.badlogicgames.gdx:gdx-freetype-platform:${libs.versions.gdx.get()}:natives-desktop")

    // Headless backend for AudioManager tests (no GL context required)
    testImplementation(libs.gdx.backend.headless)
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "17"
}

sourceSets.named("test") {
    resources {
        srcDir(file("${rootProject.projectDir}/assets"))
    }
}

tasks.named<Test>("test") {
    useJUnitPlatform()
    workingDir = file("${layout.buildDirectory.get()}/resources/test")
}
