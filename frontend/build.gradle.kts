plugins {
    base
}

val packageJson = layout.projectDirectory.file("package.json").asFile
val isWindows = System.getProperty("os.name").lowercase().contains("win")

val npmInstall by tasks.registering(Exec::class) {
    group = "build"
    description = "Install frontend dependencies"
    workingDir = projectDir
    if (isWindows) {
        commandLine("cmd", "/c", "npm install")
    } else {
        commandLine("/bin/zsh", "-lc", "npm install")
    }
    onlyIf { packageJson.exists() }
}

val npmBuild by tasks.registering(Exec::class) {
    group = "build"
    description = "Build React frontend"
    dependsOn(npmInstall)
    workingDir = projectDir
    if (isWindows) {
        commandLine("cmd", "/c", "npm run build")
    } else {
        commandLine("/bin/zsh", "-lc", "npm run build")
    }
    onlyIf { packageJson.exists() }
}

tasks.named("build") {
    dependsOn(npmBuild)
}

tasks.named<Delete>("clean") {
    delete("dist")
}
