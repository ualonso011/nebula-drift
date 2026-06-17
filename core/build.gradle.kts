plugins {
    alias(libs.plugins.kotlin.jvm)
    `java-library`
}

dependencies {
    // libGDX
    api(libs.gdx)

    // KTX
    api(libs.ktx.app)
    api(libs.ktx.graphics)
    api(libs.ktx.assets)
    api(libs.ktx.math)
    api(libs.ktx.scene2d)

    // Testing
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "17"
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}
