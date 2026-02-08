plugins {
    kotlin("jvm")
    alias(libs.plugins.maven.publish)
}

dependencies {
    implementation(libs.symbol.processing.api)
}

mavenPublishing {
    publishToMavenCentral()
    signAllPublications()
    coordinates("io.github.deference3", "formdoc-processor", "0.0.5")
    pom {
        name = "Form Doc"
        description = "A Compose Multiplatform form validating library."
        inceptionYear = "2026"
        url = "https://github.com/DeFerence3/form-doctor"
        licenses {
            license {
                name = "The Apache License, Version 2.0"
                url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                distribution = "https://www.apache.org/licenses/LICENSE-2.0.txt"
            }
        }
        developers {
            developer {
                id = "abhishek-deference"
                name = "Abhishek Krishnan"
                url = "https://github.com/DeFerence3"
            }
        }
        scm {
            url = "https://github.com/DeFerence3/form-doctor"
            connection = "scm:git:git://github.com/DeFerence3/form-doctor.git"
            developerConnection = "scm:git:ssh://git@github.com/DeFerence3/form-doctor.git"
        }
    }
}