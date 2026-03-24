import org.gradle.api.credentials.PasswordCredentials
import org.gradle.authentication.http.BasicAuthentication

rootProject.name = "ai-projects"

val excludedDirNames = setOf(".git", ".gradle", "build", "node_modules")
val usedModuleNames = mutableSetOf<String>()

rootDir
	.walkTopDown()
	.onEnter { directory -> directory == rootDir || directory.name !in excludedDirNames }
	.filter { directory ->
		directory.isDirectory &&
			directory != rootDir &&
			file("${directory.relativeTo(rootDir).invariantSeparatorsPath}/build.gradle.kts").isFile
	}
	.sortedBy { directory -> directory.relativeTo(rootDir).invariantSeparatorsPath }
	.forEach { moduleDir ->
		val baseModuleName = moduleDir.name
		var moduleName = baseModuleName
		var duplicateIndex = 2
		while (!usedModuleNames.add(moduleName)) {
			moduleName = "$baseModuleName-$duplicateIndex"
			duplicateIndex += 1
		}

		val modulePath = ":$moduleName"
		include(modulePath)
		project(modulePath).projectDir = moduleDir
	}

val artifactoryMavenUrl = providers
	.gradleProperty("artifactoryMavenUrl")
	.orElse(providers.environmentVariable("ARTIFACTORY_MAVEN_URL"))
	.orNull

val artifactoryPluginUrl = providers
	.gradleProperty("artifactoryPluginUrl")
	.orElse(providers.environmentVariable("ARTIFACTORY_PLUGIN_URL"))
	.orElse(providers.gradleProperty("artifactoryMavenUrl"))
	.orElse(providers.environmentVariable("ARTIFACTORY_MAVEN_URL"))
	.orNull

val artifactoryUsername = providers
	.gradleProperty("artifactoryUsername")
	.orElse(providers.environmentVariable("ARTIFACTORY_USERNAME"))
	.orNull

val artifactoryPassword = providers
	.gradleProperty("artifactoryPassword")
	.orElse(providers.environmentVariable("ARTIFACTORY_PASSWORD"))
	.orNull

pluginManagement {
	repositories {
		val pluginRepoUrl = providers
			.gradleProperty("artifactoryPluginUrl")
			.orElse(providers.environmentVariable("ARTIFACTORY_PLUGIN_URL"))
			.orElse(providers.gradleProperty("artifactoryMavenUrl"))
			.orElse(providers.environmentVariable("ARTIFACTORY_MAVEN_URL"))
			.orNull
		val pluginRepoUsername = providers
			.gradleProperty("artifactoryUsername")
			.orElse(providers.environmentVariable("ARTIFACTORY_USERNAME"))
			.orNull
		val pluginRepoPassword = providers
			.gradleProperty("artifactoryPassword")
			.orElse(providers.environmentVariable("ARTIFACTORY_PASSWORD"))
			.orNull

		if (!pluginRepoUrl.isNullOrBlank()) {
			maven {
				name = "artifactoryPlugins"
				url = uri(pluginRepoUrl)
				if (!pluginRepoUsername.isNullOrBlank() && !pluginRepoPassword.isNullOrBlank()) {
					credentials(PasswordCredentials::class) {
						username = pluginRepoUsername
						password = pluginRepoPassword
					}
					authentication {
						create<BasicAuthentication>("basic")
					}
				}
			}
		}
		gradlePluginPortal()
		mavenCentral()
	}
}

dependencyResolutionManagement {
	repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
	repositories {
		if (!artifactoryMavenUrl.isNullOrBlank()) {
			maven {
				name = "artifactoryMaven"
				url = uri(artifactoryMavenUrl)
				if (!artifactoryUsername.isNullOrBlank() && !artifactoryPassword.isNullOrBlank()) {
					credentials(PasswordCredentials::class) {
						username = artifactoryUsername
						password = artifactoryPassword
					}
					authentication {
						create<BasicAuthentication>("basic")
					}
				}
			}
		}
		mavenCentral()
	}
}
