pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ArdourAndroid"
include(":app")

// Ardour intentionally consumes the current One UI Compose project as source,
// rather than copying components or maintaining a parallel theme.
// CI checks out Ragnarok93/oneui-compose feature/oneui8-compose-components here.
val oneUiComposeDir = providers.gradleProperty("oneuiComposeDir")
    .orNull
    ?: System.getenv("ONEUI_COMPOSE_DIR")
    ?: file("oneui-compose-src").absolutePath

val oneUiComposeBuild = file(oneUiComposeDir)
check(oneUiComposeBuild.exists()) {
    "OneUI Compose checkout not found at $oneUiComposeBuild. " +
        "Clone Ragnarok93/oneui-compose feature/oneui8-compose-components " +
        "or pass -PoneuiComposeDir=/path/to/oneui-compose."
}

includeBuild(oneUiComposeBuild) {
    dependencySubstitution {
        substitute(module("com.github.Ragnarok93:oneui-compose"))
            .using(project(":lib"))
    }
}
