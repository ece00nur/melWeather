plugins {
    id("com.android.application") version "9.4.0" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.2.10" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
    id("org.sonarqube") version "5.1.0.4882"
}

sonarqube {
    properties {
        property("sonar.projectKey", "ece00nur_melWeather")
        property("sonar.organization", "ece00nur")
        property("sonar.host.url", "https://sonarcloud.io")
    }
}
