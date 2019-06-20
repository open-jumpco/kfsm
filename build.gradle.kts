plugins {
    kotlin("jvm") version "1.3.31"
}

repositories {
    mavenCentral()
}

group = "com.github.corneil"

version = "0.0.1"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

dependencies {
    implementation(kotlin("stdlib"))
    testImplementation(kotlin("test"))
    testImplementation(kotlin("test-junit"))
}



