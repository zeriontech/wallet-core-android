import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    repositories {
        mavenCentral()
        google()
    }

    dependencies {
        classpath(Dependencies.Gradle.gradle)
        classpath(Dependencies.Gradle.gradlePlugin)
    }
}

allprojects {
    repositories {
        mavenCentral()
        gradlePluginPortal()
        google()
        maven("https://www.jitpack.io")
        maven("https://repo.spring.io/ui/native/plugins-release/")
        mavenLocal()
        maven {
            url = uri("https://maven.pkg.github.com/trustwallet/wallet-core")
            credentials {
                username = project.findProperty("gpr.user") as String ?: System.getenv("USERNAME")
                password = project.findProperty("gpr.key") as String ?: System.getenv("TOKEN")
            }
        }
    }
}

tasks.register<Delete>("clean") {
    delete(rootProject.buildDir)
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = JavaVersion.VERSION_11.toString()
}
