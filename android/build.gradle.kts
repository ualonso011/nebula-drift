plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.nebuladrift"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.nebuladrift"
        minSdk = 21
        targetSdk = 34
        versionCode = 4
        versionName = "0.4.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }

    // Configure native libraries extraction
    sourceSets {
        getByName("main") {
            jniLibs.srcDir("src/main/jniLibs")
        }
    }
}

// Configuration for native libraries
val natives: Configuration by configurations.creating

dependencies {
    implementation(project(":core"))
    implementation(libs.gdx.backend.android)
    
    // Native libraries for libGDX
    natives("com.badlogicgames.gdx:gdx-platform:1.12.1:natives-arm64-v8a")
    natives("com.badlogicgames.gdx:gdx-platform:1.12.1:natives-armeabi-v7a")
    natives("com.badlogicgames.gdx:gdx-platform:1.12.1:natives-x86_64")
}

// Task to extract native libraries from JARs
tasks.register("extractNatives") {
    doLast {
        val jniLibsDir = file("$projectDir/src/main/jniLibs")
        jniLibsDir.deleteRecursively()
        
        natives.files.forEach { jar ->
            val arch = when {
                jar.name.contains("arm64-v8a") -> "arm64-v8a"
                jar.name.contains("armeabi-v7a") -> "armeabi-v7a"
                jar.name.contains("x86_64") -> "x86_64"
                jar.name.contains("x86") -> "x86"
                else -> throw GradleException("Unknown architecture in ${jar.name}")
            }
            
            val archDir = file("$jniLibsDir/$arch")
            archDir.mkdirs()
            
            copy {
                from(zipTree(jar))
                into(archDir)
                include("*.so")
            }
        }
    }
}

// Make sure natives are extracted before building
tasks.named("preBuild") {
    dependsOn("extractNatives")
}
