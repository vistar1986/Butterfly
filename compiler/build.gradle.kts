plugins {
    id("kotlin")
    id("kotlin-kapt")
    id("maven-publish")
}

group = "com.github.ssseasonnn"

dependencies {
    implementation(project(":annotation"))
    implementation("com.squareup:kotlinpoet:1.10.2")

    implementation("com.google.auto.service:auto-service:1.0")
    kapt("com.google.auto.service:auto-service:1.0")
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            from(components.getByName("java"))
        }
    }
}