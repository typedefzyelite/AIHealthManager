import java.util.Properties
import java.io.FileInputStream

pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

val localProperties = Properties()
val localPropertiesFile = file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

fun localProp(key: String, default: String = ""): String =
    localProperties.getProperty(key, default)

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://jitpack.io") }
        val healthMavenUser = localProp("HEYTAP_HEALTH_MAVEN_USER")
        val healthMavenPassword = localProp("HEYTAP_HEALTH_MAVEN_PASSWORD")
        if (healthMavenUser.isNotEmpty() && healthMavenPassword.isNotEmpty()) {
            maven {
                url = uri("https://maven.columbus.heytapmobi.com/repository/heytap-health-releases/")
                isAllowInsecureProtocol = true
                credentials {
                    username = healthMavenUser
                    password = healthMavenPassword
                }
            }
            maven {
                url = uri("https://maven.columbus.heytapmobi.com/repository/heytap-health-snapshots/")
                isAllowInsecureProtocol = true
                credentials {
                    username = healthMavenUser
                    password = healthMavenPassword
                }
            }
        }
    }
}

rootProject.name = "AIHealthManager_2"
include(":app")
